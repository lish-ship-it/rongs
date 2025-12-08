// Engine_Rongs.sc
// Rongs - Rings + Rom = Rongs
// Polyphonic resonator with COSMOS-style delays and granular reverb
// For Norns + Grid + Arc

Engine_Rongs : CroneEngine {
    classvar <numVoices = 4;
    
    var <synths;           // Array of voice synths
    var <voiceNotes;       // Track which note each voice is playing
    var <voiceGates;       // Track gate state per voice
    var <nextVoice = 0;    // Round-robin voice allocation
    
    var <fxGroup;          // Group for effects
    var <voiceGroup;       // Group for voices
    var <fxSynth;          // Effects chain synth
    
    var <ringsBus;         // Bus from voices to effects
    
    // Parameters with defaults
    var <structure = 0.5;
    var <brightness = 0.5;
    var <damping = 0.5;
    var <position = 0.5;
    var <mode = 0;         // 0=modal, 1=sympathetic, 2=inharmonic
    
    var <attack = 0.01;
    var <decay = 0.5;
    
    var <bitDepth = 24;
    var <sampleRateVal = 48000;
    
    var <blur = 0.0;
    var <drift = 0.0;
    var <feedback = 0.5;
    var <delayMix = 0.3;
    var <delaySize = 1;    // 0=small, 1=medium, 2=large, 3=huge
    
    var <revDecay = 0.5;
    var <texture = 0.5;
    var <density = 100;
    var <revMix = 0.3;
    var <freeze = 0;
    
    var <amDepth = 0.0;
    var <fmDepth = 0.0;
    
    var <inputLevel = 0.0;
    var <masterLevel = 0.7;
    
    *new { arg context, doneCallback;
        ^super.new(context, doneCallback);
    }
    
    alloc {
        // Allocate buses
        ringsBus = Bus.audio(context.server, 2);
        
        // Create groups for ordering
        voiceGroup = Group.new(context.xg);
        fxGroup = Group.after(voiceGroup);
        
        // Initialize voice tracking
        synths = Array.fill(numVoices, { nil });
        voiceNotes = Array.fill(numVoices, { -1 });
        voiceGates = Array.fill(numVoices, { 0 });
        
        // =====================
        // SYNTHDEFS
        // =====================
        
        // Voice SynthDef using MiRings
        SynthDef(\rtg_voice, {
            arg out, gate = 1, freq = 440, vel = 1,
                structure = 0.5, brightness = 0.5, damping = 0.5, position = 0.5,
                attack = 0.01, decay = 0.5, mode = 0;
            
            var trig, env, sig, pit, excite;
            
            // Excitation envelope
            env = EnvGen.kr(
                Env.perc(attack, decay, 1, -4),
                gate,
                doneAction: 2
            );
            
            // Trigger on gate
            trig = Trig1.kr(gate, 0.01);
            
            // Pitch as MIDI note
            pit = freq.cpsmidi;
            
            // Excitation signal - filtered noise burst
            excite = WhiteNoise.ar(0.5) * env * vel;
            excite = HPF.ar(excite, 200);
            
            // MiRings - modal resonator
            sig = MiRings.ar(
                in: excite,
                trig: trig,
                pit: pit,
                struct: structure,
                bright: brightness,
                damp: damping,
                pos: position,
                model: mode,
                poly: 1,           // Internal polyphony off
                intern_exciter: 0  // Use our excitation
            );
            
            // MiRings outputs [odd, even] partials - position controls L/R balance
            // position=0.5 gives equal stereo spread
            Out.ar(out, [sig[0], sig[1]]);
        }).add;
        
        // Effects chain SynthDef
        SynthDef(\rtg_fx, {
            arg in, out,
                // Lo-fi
                bitDepth = 24, sampleRateR = 48000,
                // Delays
                blur = 0.0, drift = 0.0, feedback = 0.5, delayMix = 0.3,
                delayMult = 1.0,  // Multiplier for delay times
                // Reverb
                revDecay = 0.5, texture = 0.5, density = 100, revMix = 0.3, freeze = 0,
                // Modulation
                amDepth = 0.0, fmDepth = 0.0,
                // Mix
                inputLevel = 0.0, masterLevel = 0.7;
            
            var sig, input, dry;
            var del1, del2, del3, del4, delOut, localIn;
            var grainSig, wet;
            var amMod, fmMod;
            var lfo1, lfo2, lfo3, lfo4;
            var driftMod;
            var baseTime1, baseTime2, baseTime3, baseTime4;
            
            // Read Rings output
            sig = In.ar(in, 2);
            
            // Read external input and mix
            input = SoundIn.ar([0, 1]) * inputLevel;
            sig = sig + input;
            
            dry = sig;
            
            // =====================
            // LO-FI PROCESSING
            // =====================
            sig = Decimator.ar(sig, sampleRateR, bitDepth);
            
            // =====================
            // DRIFTING DELAYS (COSMOS-style)
            // Base times scaled by delayMult:
            // Small (1x): ~2.5s total
            // Medium (2x): ~5s total  
            // Large (3x): ~7.5s total
            // =====================
            baseTime1 = 0.625 * delayMult;
            baseTime2 = 0.787 * delayMult;
            baseTime3 = 0.921 * delayMult;
            baseTime4 = 1.103 * delayMult;
            
            driftMod = LFNoise1.kr(0.1) * drift;
            
            lfo1 = SinOsc.kr(0.13 + (driftMod * 0.1)) * drift;
            lfo2 = SinOsc.kr(0.17 + (driftMod * 0.08)) * drift;
            lfo3 = SinOsc.kr(0.11 + (driftMod * 0.12)) * drift;
            lfo4 = SinOsc.kr(0.19 + (driftMod * 0.07)) * drift;
            
            // Local feedback for blur
            localIn = LocalIn.ar(2);
            
            // Four delay lines with prime-ratio times
            // Max delay time = 4 seconds to fit in memory
            del1 = DelayC.ar(
                sig[0] + (localIn[0] * blur),
                4.0,
                (baseTime1 + (lfo1 * 0.02 * delayMult)).clip(0.001, 3.9)
            );
            del2 = DelayC.ar(
                sig[1] + (localIn[1] * blur),
                4.0,
                (baseTime2 + (lfo2 * 0.02 * delayMult)).clip(0.001, 3.9)
            );
            del3 = DelayC.ar(
                (sig[0] * 0.5) + (del1 * blur * 0.5),
                4.0,
                (baseTime3 + (lfo3 * 0.02 * delayMult)).clip(0.001, 3.9)
            );
            del4 = DelayC.ar(
                (sig[1] * 0.5) + (del2 * blur * 0.5),
                4.0,
                (baseTime4 + (lfo4 * 0.02 * delayMult)).clip(0.001, 3.9)
            );
            
            // Cross-feedback
            LocalOut.ar([(del3 + del4) * feedback, (del1 + del2) * feedback]);
            
            // Mix delays with panning
            delOut = [
                (del1 * (1 + lfo1)) + (del3 * (1 - lfo3)),
                (del2 * (1 - lfo2)) + (del4 * (1 + lfo4))
            ] * 0.25;
            
            // Delay wet/dry
            sig = (sig * (1 - delayMix)) + (delOut * delayMix);
            
            // =====================
            // AM MODULATION (pre-reverb)
            // =====================
            amMod = SinOsc.kr(
                LFNoise1.kr(0.2).range(0.5, 4)
            ).range(1 - amDepth, 1);
            sig = sig * amMod;
            
            // =====================
            // GRANULAR REVERB with FREEZE
            // Simplified for CPU - 2 combs instead of 4
            // =====================
            grainSig = sig;
            
            // Texture randomizes comb times
            grainSig = Mix.ar([
                CombC.ar(grainSig, 0.25, 
                    (density / 1000) + (LFNoise1.kr(1.5) * texture * 0.03), 
                    revDecay * 6),
                CombC.ar(grainSig, 0.25, 
                    (density / 1000 * 1.23) + (LFNoise1.kr(1.1) * texture * 0.03), 
                    revDecay * 6)
            ]) * 0.4;
            
            // Allpass diffusion - reduced to 2 stages for CPU
            2.do { |i|
                grainSig = AllpassC.ar(grainSig, 0.15, 
                    [0.035, 0.047, 0.071, 0.087].wrapAt(i * 2 + [0, 1]) 
                        + (LFNoise1.kr([0.4, 0.5]) * texture * 0.008),
                    revDecay * 3
                );
            };
            
            // Freeze feedback loop
            grainSig = grainSig + (LocalIn.ar(2, 0) * freeze * 0.985);
            LocalOut.ar(grainSig * freeze);
            
            // Reverb wet/dry
            wet = grainSig;
            sig = (sig * (1 - revMix)) + (wet * revMix);
            
            // =====================
            // FM MODULATION (post-reverb)
            // Uses wet/dry blend - at fmDepth=0, fully bypassed
            // =====================
            fmMod = LFNoise1.kr(
                LFNoise1.kr(0.3).range(0.2, 2)
            ) * 0.015;
            
            sig = (sig * (1 - fmDepth)) + (DelayC.ar(sig, 0.1, fmMod.abs + 0.001) * fmDepth);
            
            // =====================
            // OUTPUT
            // =====================
            sig = LeakDC.ar(sig);
            sig = sig * masterLevel;
            sig = Limiter.ar(sig, 0.95);
            
            Out.ar(out, sig);
        }).add;
        
        context.server.sync;
        
        // Start the effects synth
        fxSynth = Synth.new(\rtg_fx, [
            \in, ringsBus,
            \out, context.out_b,
            \bitDepth, bitDepth,
            \sampleRateR, sampleRateVal,
            \blur, blur,
            \drift, drift,
            \feedback, feedback,
            \delayMix, delayMix,
            \delayMult, [1, 2, 3].at(delaySize.clip(0, 2)),
            \revDecay, revDecay,
            \texture, texture,
            \density, density,
            \revMix, revMix,
            \freeze, freeze,
            \amDepth, amDepth,
            \fmDepth, fmDepth,
            \inputLevel, inputLevel,
            \masterLevel, masterLevel
        ], fxGroup);
        
        // =====================
        // COMMANDS
        // =====================
        
        this.addCommand(\noteOn, "if", { arg msg;
            var note = msg[1].asInteger;
            var vel = msg[2].asFloat;
            this.noteOn(note, vel);
        });
        
        this.addCommand(\noteOff, "i", { arg msg;
            var note = msg[1].asInteger;
            this.noteOff(note);
        });
        
        this.addCommand(\structure, "f", { arg msg;
            structure = msg[1].asFloat;
            synths.do { |s| if(s.notNil) { s.set(\structure, structure) } };
        });
        
        this.addCommand(\brightness, "f", { arg msg;
            brightness = msg[1].asFloat;
            synths.do { |s| if(s.notNil) { s.set(\brightness, brightness) } };
        });
        
        this.addCommand(\damping, "f", { arg msg;
            damping = msg[1].asFloat;
            synths.do { |s| if(s.notNil) { s.set(\damping, damping) } };
        });
        
        this.addCommand(\position, "f", { arg msg;
            position = msg[1].asFloat;
            synths.do { |s| if(s.notNil) { s.set(\position, position) } };
        });
        
        this.addCommand(\mode, "i", { arg msg;
            mode = msg[1].asInteger.clip(0, 2);
            synths.do { |s| if(s.notNil) { s.set(\mode, mode) } };
        });
        
        this.addCommand(\attack, "f", { arg msg;
            attack = msg[1].asFloat.max(0.001);
        });
        
        this.addCommand(\decay, "f", { arg msg;
            decay = msg[1].asFloat.max(0.001);
        });
        
        this.addCommand(\bitDepth, "f", { arg msg;
            bitDepth = msg[1].asFloat.clip(2, 24);
            fxSynth.set(\bitDepth, bitDepth);
        });
        
        this.addCommand(\sampleRate, "f", { arg msg;
            sampleRateVal = msg[1].asFloat.clip(2000, 48000);
            fxSynth.set(\sampleRateR, sampleRateVal);
        });
        
        this.addCommand(\blur, "f", { arg msg;
            blur = msg[1].asFloat;
            fxSynth.set(\blur, blur);
        });
        
        this.addCommand(\drift, "f", { arg msg;
            drift = msg[1].asFloat;
            fxSynth.set(\drift, drift);
        });
        
        this.addCommand(\feedback, "f", { arg msg;
            feedback = msg[1].asFloat.clip(0, 1.2);
            fxSynth.set(\feedback, feedback);
        });
        
        this.addCommand(\delayMix, "f", { arg msg;
            delayMix = msg[1].asFloat;
            fxSynth.set(\delayMix, delayMix);
        });
        
        this.addCommand(\delaySize, "i", { arg msg;
            delaySize = msg[1].asInteger.clip(0, 2);
            fxSynth.set(\delayMult, [1, 2, 3].at(delaySize));
        });
        
        this.addCommand(\revDecay, "f", { arg msg;
            revDecay = msg[1].asFloat;
            fxSynth.set(\revDecay, revDecay);
        });
        
        this.addCommand(\texture, "f", { arg msg;
            texture = msg[1].asFloat;
            fxSynth.set(\texture, texture);
        });
        
        this.addCommand(\density, "f", { arg msg;
            density = msg[1].asFloat.clip(50, 200);
            fxSynth.set(\density, density);
        });
        
        this.addCommand(\revMix, "f", { arg msg;
            revMix = msg[1].asFloat;
            fxSynth.set(\revMix, revMix);
        });
        
        this.addCommand(\freeze, "i", { arg msg;
            freeze = msg[1].asInteger.clip(0, 1);
            fxSynth.set(\freeze, freeze);
        });
        
        this.addCommand(\amDepth, "f", { arg msg;
            amDepth = msg[1].asFloat;
            fxSynth.set(\amDepth, amDepth);
        });
        
        this.addCommand(\fmDepth, "f", { arg msg;
            fmDepth = msg[1].asFloat;
            fxSynth.set(\fmDepth, fmDepth);
        });
        
        this.addCommand(\inputLevel, "f", { arg msg;
            inputLevel = msg[1].asFloat;
            fxSynth.set(\inputLevel, inputLevel);
        });
        
        this.addCommand(\masterLevel, "f", { arg msg;
            masterLevel = msg[1].asFloat;
            fxSynth.set(\masterLevel, masterLevel);
        });
    }
    
    noteOn { arg note, vel;
        var voiceIdx;
        var freq = note.midicps;
        
        // Look for free voice
        voiceIdx = voiceGates.indexOf(0);
        
        // Steal oldest if none free
        if(voiceIdx.isNil) {
            voiceIdx = nextVoice;
            if(synths[voiceIdx].notNil) {
                synths[voiceIdx].set(\gate, 0);
            };
        };
        
        // Create voice
        synths[voiceIdx] = Synth.new(\rtg_voice, [
            \out, ringsBus,
            \gate, 1,
            \freq, freq,
            \vel, vel,
            \structure, structure,
            \brightness, brightness,
            \damping, damping,
            \position, position,
            \attack, attack,
            \decay, decay,
            \mode, mode
        ], voiceGroup);
        
        voiceNotes[voiceIdx] = note;
        voiceGates[voiceIdx] = 1;
        nextVoice = (nextVoice + 1) % numVoices;
    }
    
    noteOff { arg note;
        numVoices.do { |i|
            if(voiceNotes[i] == note) {
                if(synths[i].notNil) {
                    synths[i].set(\gate, 0);
                };
                voiceNotes[i] = -1;
                voiceGates[i] = 0;
            };
        };
    }
    
    free {
        synths.do { |s| if(s.notNil) { s.free } };
        if(fxSynth.notNil) { fxSynth.free };
        if(voiceGroup.notNil) { voiceGroup.free };
        if(fxGroup.notNil) { fxGroup.free };
        if(ringsBus.notNil) { ringsBus.free };
    }
}
