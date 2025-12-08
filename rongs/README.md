# Rongs

**Rings + Rom = Rongs**

A polyphonic resonator instrument for Norns combining Mutable Instruments Rings synthesis with COSMOS-style drifting delays and Pladask ROM-inspired granular reverb.

## Requirements

- Norns (or Norns Shield)
- Grid 128 (required)
- Arc (optional)
- mi-engines (install via Maiden)

## Installation

1. Download or clone to `~/dust/code/rongs`
2. Install mi-engines if not already present:
   ```
   ;install https://github.com/okyeron/mi-engines
   ```
3. Restart Norns

## Controls

### Grid (16×8)

**Rows 1-6: Chromatic Keyboard**
- Bottom-left = C2, ascending chromatically
- Bright LEDs = white keys, Dim LEDs = black keys
- Brightest = currently held note

**Row 7: Mode & Controls**
- Cols 1-3: Rings mode (Modal / Sympathetic / Inharmonic)
- Cols 5-6: Arc page select
- Cols 12-16: Freeze (hold)

**Row 8: Quick Presets**
- Cols 1-4: Lo-fi (clean → crushed)
- Cols 6-8: Delay feedback (short → infinite)
- Cols 10-12: AM depth
- Cols 14-16: FM depth

### Arc (Optional)

**Page 1: Sound Source**
- Arc 1: Structure
- Arc 2: Brightness
- Arc 3: Damping
- Arc 4: Position

**Page 2: Effects**
- Arc 1: Blur
- Arc 2: Drift
- Arc 3: Texture
- Arc 4: Reverb Mix

### Norns

**Encoders**
- E1: Master level
- E2: Delay mix (K1+E2: Input level)
- E3: Density (K1+E3: Feedback)

**Keys**
- K1: Shift
- K2: Toggle Arc page
- K3: Freeze

### MIDI

- Note on/off triggers voices
- Velocity controls excitation intensity
- Channel selectable in params (0 = omni)

## Delay Sizes

The COSMOS-style delay can be set to different total lengths:
- **Small**: ~2.5 seconds (original COSMOS "small")
- **Medium**: ~5 seconds (default)
- **Large**: ~7.5 seconds

Longer delays create more expansive, evolving textures.

## Signal Flow

```
Grid/MIDI → Rings (4-voice) → Envelope → Lo-Fi
                                           ↓
Audio In ────────────────────────────────► Mix
                                           ↓
                                    Drifting Delays
                                           ↓
                                       AM Mod
                                           ↓
                                   Granular Reverb ← Freeze
                                           ↓
                                       FM Mod
                                           ↓
                                       Output
```

## CPU Notes

This is a CPU-intensive script due to MiRings polyphony. If you experience dropouts:
- Reduce polyphony by playing fewer simultaneous notes
- Use shorter attack/decay envelopes
- Reduce delay feedback

## Credits

- Mutable Instruments Rings algorithm by Émilie Gillet
- mi-engines SuperCollider ports by okyeron
- Inspired by SOMA COSMOS and Pladask ROM

## License

GPL-3.0
