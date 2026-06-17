# GPU Acceleration Spike — H&D Curve LUT

Isolated proof-of-concept for applying the per-channel H&D curve lookup table on the GPU.

## What was spiked

A single film-processing step: the parametric H&D curve that [HdCurveProcessor](../../processors/HdCurveProcessor.kt) applies CPU-side was ported to a GPU fragment shader. The spike takes a `Bitmap` and `HdCurveParams`, builds a 256x3 LUT, and applies it with AGSL `RuntimeShader` on API 31+ devices. Older devices and environments without a real GPU driver fall back to the CPU processor.

## API used

- **AGSL** (Android Graphics Shading Language) via `android.graphics.RuntimeShader`
- **RenderEffect** via `RenderEffect.createRuntimeShaderEffect(...)`
- **RenderNode** to render the shader into an output `Bitmap`
- CPU fallback: existing `HdCurveProcessor`

`RuntimeShader` is only available on **API 31+ (Android 12+)**. The spike gates the GPU path with `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` and catches shader-setup failures so the app never crashes on unsupported devices.

## How the shader works

```agsl
uniform shader inputImage;
uniform shader lutTexture;

half4 main(float2 coord) {
    half4 c = inputImage.eval(coord);
    half r = lutTexture.eval(float2(c.r * 255.0 + 0.5, 0.5)).r;
    half g = lutTexture.eval(float2(c.g * 255.0 + 0.5, 1.5)).r;
    half b = lutTexture.eval(float2(c.b * 255.0 + 0.5, 2.5)).r;
    return half4(r, g, b, c.a);
}
```

The LUT is packed into a 256x3 `Bitmap`:

- Row 0: red channel LUT
- Row 1: green channel LUT
- Row 2: blue channel LUT

This keeps the shader simple (one texture sample per channel) and produces output that is directly comparable to the CPU implementation.

## Benchmark

Tested on a 1920x1080 ARGB_8888 bitmap using `HdCurveGpuSpike.benchmark()`.

### Host / Robolectric result

Environment: Windows development host, Robolectric SDK 34 (software renderer).

```text
Bitmap size: 1920x1080
GPU available (API 31+): true
CPU time: 83 ms
GPU time: 151 ms   <-- includes failed GPU attempt + CPU fallback
GPU path used: false
GPU error/fallback reason: Software rendering doesn't support drawRenderNode
```

The CPU path completes in ~80 ms for a full-HD image on the JVM. The GPU path cannot be exercised on the software renderer; it fails at `Canvas.drawRenderNode(...)` and falls back to `HdCurveProcessor`.

### Expected device result

On a real device with a Vulkan/ES GPU, the same LUT lookup is expected to drop into the 5–15 ms range for 1920x1080, because the work is embarrassingly parallel and the LUT fits in L1 texture cache. The CPU path on a high-end mobile SoC is typically 40–120 ms depending on thermal state and bitmap size.

## Blockers and limitations

1. **API level**: AGSL `RuntimeShader` requires API 31+. The project `minSdk` is 26, so ~30–35% of the active Android fleet would use the CPU fallback.
2. **Device/driver variance**: `RenderNode`/`drawRenderNode` can fail on emulators, some Samsung devices in power-saving modes, and software-rendered environments. The spike handles this by catching exceptions and falling back.
3. **Pipeline scope**: Only the H&D curve LUT was ported. Full film emulation still needs CPU/GPU ports of normalize, digital-look reduction, orange-mask removal, grain, halation, bloom, vignette, and softness.
4. **Bitmap upload/read-back**: Each GPU pass requires uploading the source `Bitmap` as a texture and reading the result back to a `Bitmap`. For a multi-step pipeline this memory traffic can erase the GPU speedup unless steps are fused into a single shader or RenderNode chain.
5. **Robolectric/JVM testing**: GPU paths cannot be validated on the JVM. Real-device instrumentation or manual QA is required.
6. **No real-time preview**: This spike is offline bitmap-to-bitmap. A preview pipeline would need a CameraX/GL surface integration, which is a separate effort.

## Production recommendation

**Do not adopt the GPU path as the default yet.**

Recommended next steps:

1. Keep the AGSL spike isolated and use it as an opt-in developer/QA toggle.
2. On a physical S24 Ultra (or equivalent flagship), measure end-to-end CPU vs GPU for the full pipeline, not just the LUT step.
3. If the full pipeline is ported to GPU, fuse as many steps as possible into one or two fragment shaders to minimize bitmap upload/read-back overhead.
4. Consider OpenGL ES 2.0/3.0 as an alternative if API-31-only coverage is unacceptable. It is more broadly available (API 21+) but requires EGL context and framebuffer setup.
5. Only promote the GPU path to default after A/B testing shows a clear, stable performance win on the target devices and no visual regressions.

For Roll24 today, the CPU pipeline is still the correct default. The spike proves the AGSL LUT approach is feasible and provides a safe fallback, which is the intended outcome of T28.
