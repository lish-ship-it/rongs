# Rongs

A polyphonic resonator instrument for [Norns](https://monome.org/docs/norns/) combining Mutable Instruments Rings emulation with COSMOS-style drifting delays and ROM-inspired granular reverb.

## Overview

Rongs (Rings + ROM) is a 4-voice physical modeling synthesizer built around the MiRings engine from [@okyeron](https://github.com/okyeron)'s [mi-engines](https://github.com/okyeron/mi-engines) library. It pairs the iconic resonator sound with a lush effects chain inspired by SOMA Cosmos and Pladask Elektrisk ROM.

**Key Features:**
- 4-voice polyphony with voice stealing
- 5 excitation sources (Noise, Impulse, Sine, Saw, Square)
- 3 resonator modes (Modal, Sympathetic, Inharmonic)
- Organic "instability" parameter inspired by Ciat-Lonbarde instruments
- COSMOS-style drifting delays with panoramic modulation
- ROM-style granular reverb with freeze
- Grid keyboard support (optional)
- Arc parameter control (optional)
- MIDI input support

## Requirements

- Norns (tested on stock and shield)
- [mi-engines](https://github.com/okyeron/mi-engines) by @okyeron (required)
- Grid 128 (optional, for chromatic keyboard)
- Arc (optional, for parameter control)

## Installation

1. **Install mi-engines first** (if not already installed):
   ```
   ;install https://github.com/okyeron/mi-engines
   ```

2. **Install Rongs:**
   ```
   ;install https://github.com/lish-ship-it/rongs
   ```

3. **Restart Norns** to load the new engine.

## Controls

### Norns Keys & Encoders

| Control | Function |
|---------|----------|
| **E1** | Master level |
| **E2** | Structure |
| **E3** | Brightness |
| **K2** | Freeze toggle |
| **K3** | Randomize preset |

### Grid (128 varibright)

```
┌─────────────────────────────────────────────────────────┐
│ Rows 1-2: Mode select, Delay size, Presets              │
│ Rows 3-8: Chromatic keyboard (C2-D#5)                   │
│           White keys: brightness 5                      │
│           Black keys: brightness 2                      │
│           C notes: brightness 8 (reference)             │
│           Held notes: brightness 15                     │
└─────────────────────────────────────────────────────────┘
```

### Arc (optional)

| Ring | Parameter |
|------|-----------|
| 1 | Structure |
| 2 | Brightness |
| 3 | Delay Mix |
| 4 | Reverb Mix |

## Parameters

### Exciter
- **Type**: Noise, Impulse, Sine, Saw, Square
- **Sustain**: Blend percussive/sustained envelope (for tonal exciters)

### Envelope
- **Attack**: 1ms - 2s
- **Decay**: 1ms - 5s  
- **Instability**: Organic variation (attack time, velocity, chatter, jitter)

### Resonator (MiRings)
- **Mode**: Modal, Sympathetic, Inharmonic
- **Structure**: Chord/interval control
- **Brightness**: Spectral content
- **Damping**: Decay character
- **Position**: Stereo spread / harmonic balance

### Lo-Fi
- **Bypass**: On/Off
- **Bit Depth**: 2-24 bits
- **Sample Rate**: 2kHz-48kHz

### Delays (COSMOS-style)
- **Bypass**: On/Off
- **Blur**: Cross-feedback amount
- **Drift**: Panoramic LFO modulation
- **Feedback**: Delay regeneration (0-120%)
- **Delay Mix**: Wet/dry
- **Delay Size**: Small (~2.5s) / Medium (~5s) / Large (~7.5s)

### Reverb (ROM-style granular)
- **Bypass**: On/Off
- **Decay**: Reverb tail length
- **Texture**: LPF + saturation (ROM-style degradation)
- **Density**: Grain spacing
- **Reverb Mix**: Wet/dry

### Modulation
- **Bypass**: On/Off
- **AM Depth**: Amplitude modulation
- **FM Depth**: Frequency modulation (wow/flutter)

### Input/Output
- **Ext Input Level**: External audio input
- **Master Level**: Output volume

## Exciter Types

The excitation source dramatically changes the resonator character:

| Type | Character | Best For |
|------|-----------|----------|
| **Noise** | Shimmery, metallic, bell-like | Classic Rings sound |
| **Impulse** | Sharp pluck, string-like attack | Plucked strings, harps |
| **Sine** | Smooth, bowed, synth-like | Pads, sustained tones |
| **Saw** | Bright, aggressive harmonics | Strings, brass-like |
| **Square** | Hollow, odd harmonics | Clarinet, woodwind |

### Sustain Parameter

Controls envelope behavior for tonal exciters (Sine/Saw/Square):

- `sustain = 0`: Percussive only (sound decays after attack)
- `sustain = 1`: Continuous excitation while key held (bowed/sustained)
- `sustain = 0.3-0.5`: Plucked attack that rings into sustained tone

## Instability

Inspired by Ciat-Lonbarde's wooden barre instruments, the instability parameter adds organic variation:

- **Attack randomization**: ±50% to +200% variation
- **Velocity randomization**: ±20% variation  
- **Attack chatter**: Random amplitude dips (8-25 Hz) simulating contact bounce
- **Sustain jitter**: Slow amplitude wobble (2-8 Hz) simulating inconsistent pressure

At `instability = 0`, behavior is pristine and predictable. At `instability = 1`, each note has unique organic character.

## CPU Notes

This is a CPU-intensive script due to MiRings polyphony. If you experience dropouts:

- Reduce polyphony by playing fewer simultaneous notes
- Use shorter attack/decay envelopes
- Reduce delay feedback
- **Use Bypass switches** to disable unused effects (Lo-Fi, Delays, Reverb, Modulation)

## Signal Flow

```
Grid/MIDI ──► Exciter ──► Envelope ──► MiRings (4 voices)
                              │
                              ▼
              ┌─────────────────────────────────┐
              │  Lo-Fi (bit crush / SR reduce)  │
              └─────────────────────────────────┘
                              │
Ext Audio In ─────────────────┤
                              ▼
              ┌─────────────────────────────────┐
              │  Drifting Delays (COSMOS-style) │
              │  - 4 delay lines                │
              │  - Panoramic drift modulation   │
              │  - Cross-feedback blur          │
              └─────────────────────────────────┘
                              │
                              ▼
              ┌─────────────────────────────────┐
              │  AM Modulation                  │
              └─────────────────────────────────┘
                              │
                              ▼
              ┌─────────────────────────────────┐
              │  Granular Reverb (ROM-style)    │
              │  - Comb filters                 │
              │  - Allpass diffusion            │
              │  - Freeze capture               │
              │  - Texture (LPF + saturation)   │
              └─────────────────────────────────┘
                              │
                              ▼
              ┌─────────────────────────────────┐
              │  FM Modulation                  │
              └─────────────────────────────────┘
                              │
                              ▼
                           Output
```

## Preset Combos

| Exciter | Sustain | Mode | Instability | Character |
|---------|---------|------|-------------|-----------|
| Noise | 0 | Modal | 0 | Classic Rings bells |
| Noise | 0 | Modal | 0.6 | Organic gamelan |
| Impulse | 0 | Sympathetic | 0.2 | Bright harp/koto |
| Sine | 0.8 | Modal | 0.3 | Bowed glass |
| Saw | 0.5 | Inharmonic | 0.4 | Dark metallic strings |
| Square | 1.0 | Sympathetic | 0.5 | Sustained clarinet choir |

## Credits

- **Engine**: Built on [mi-engines](https://github.com/okyeron/mi-engines) MiRings by [@okyeron](https://github.com/okyeron)
- **Inspiration**: Mutable Instruments Rings, SOMA Cosmos, Pladask Elektrisk ROM, Ciat-Lonbarde
- **Development**: Created with [Claude Code](https://claude.ai)

## License

MIT License - see LICENSE file for details.

## Version History

### v1.0.0
- Initial release
- 5 excitation sources with sustain parameter
- Instability feature (attack/velocity randomization, chatter, jitter)
- Effect bypass switches for CPU optimization
- Grid chromatic keyboard with visual feedback
- Arc parameter control
- MIDI support
