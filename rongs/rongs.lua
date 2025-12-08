-- rongs
-- Rings + Rom = Rongs
-- Polyphonic resonator + granular reverb
-- for Norns + Grid + Arc
--
-- Grid: chromatic keyboard + controls
-- Arc: parameter control (optional)
-- MIDI: note input
-- K2: toggle Arc page
-- K3: freeze
-- E1: master level
-- E2: delay mix (K1+E2: input level)
-- E3: density (K1+E3: feedback)

engine.name = "Rongs"

local g = grid.connect()
local a = arc.connect()
local m = midi.connect()

-- State
local held_keys = {}
local arc_page = 1
local shift = false
local freeze_state = 0
local mode = 1  -- 1=modal, 2=sympathetic, 3=inharmonic
local mode_names = {"Modal", "Sympathetic", "Inharmonic"}

-- Grid dimensions
local GRID_COLS = 16
local GRID_ROWS = 8
local KEY_ROWS = 6  -- Rows 1-6 for keyboard
local BASE_NOTE = 36  -- C2

-- Arc page parameters
local arc_params = {
    [1] = {"structure", "brightness", "damping", "position"},
    [2] = {"blur", "drift", "texture", "rev_mix"}
}

-- Preset values
local lofi_presets = {
    {bits = 24, sr = 48000},  -- Clean
    {bits = 12, sr = 24000},  -- Light
    {bits = 8, sr = 12000},   -- Medium
    {bits = 4, sr = 6000}     -- Crushed
}

local feedback_presets = {0.3, 0.7, 1.0}  -- Short, Medium, Infinite
local am_presets = {0, 0.3, 0.7}
local fm_presets = {0, 0.3, 0.7}

-----------------------
-- INIT
-----------------------
function init()
    -- Parameters
    params:add_separator("rongs_rings", "RINGS")
    
    params:add_option("mode", "Mode", mode_names, 1)
    params:set_action("mode", function(v)
        mode = v
        engine.mode(v - 1)
    end)
    
    params:add_control("structure", "Structure", controlspec.new(0, 1, "lin", 0.01, 0.5))
    params:set_action("structure", function(v) engine.structure(v) end)
    
    params:add_control("brightness", "Brightness", controlspec.new(0, 1, "lin", 0.01, 0.5))
    params:set_action("brightness", function(v) engine.brightness(v) end)
    
    params:add_control("damping", "Damping", controlspec.new(0, 1, "lin", 0.01, 0.5))
    params:set_action("damping", function(v) engine.damping(v) end)
    
    params:add_control("position", "Position", controlspec.new(0, 1, "lin", 0.01, 0.5))
    params:set_action("position", function(v) engine.position(v) end)
    
    params:add_separator("rongs_env", "ENVELOPE")
    
    params:add_control("attack", "Attack", controlspec.new(0.001, 2, "exp", 0.001, 0.01, "s"))
    params:set_action("attack", function(v) engine.attack(v) end)
    
    params:add_control("decay", "Decay", controlspec.new(0.001, 5, "exp", 0.001, 0.5, "s"))
    params:set_action("decay", function(v) engine.decay(v) end)
    
    params:add_separator("rongs_lofi", "LO-FI")
    
    params:add_control("bit_depth", "Bit Depth", controlspec.new(2, 24, "lin", 1, 24))
    params:set_action("bit_depth", function(v) engine.bitDepth(v) end)
    
    params:add_control("sample_rate", "Sample Rate", controlspec.new(2000, 48000, "exp", 100, 48000, "Hz"))
    params:set_action("sample_rate", function(v) engine.sampleRate(v) end)
    
    params:add_separator("rongs_delays", "DELAYS")
    
    params:add_control("blur", "Blur", controlspec.new(0, 1, "lin", 0.01, 0))
    params:set_action("blur", function(v) engine.blur(v) end)
    
    params:add_control("drift", "Drift", controlspec.new(0, 1, "lin", 0.01, 0))
    params:set_action("drift", function(v) engine.drift(v) end)
    
    params:add_control("feedback", "Feedback", controlspec.new(0, 1.2, "lin", 0.01, 0.5))
    params:set_action("feedback", function(v) engine.feedback(v) end)
    
    params:add_control("delay_mix", "Delay Mix", controlspec.new(0, 1, "lin", 0.01, 0.3))
    params:set_action("delay_mix", function(v) engine.delayMix(v) end)
    
    params:add_option("delay_size", "Delay Size", {"Small ~2.5s", "Medium ~5s", "Large ~7.5s"}, 2)
    params:set_action("delay_size", function(v) engine.delaySize(v - 1) end)
    
    params:add_separator("rongs_reverb", "REVERB")
    
    params:add_control("rev_decay", "Decay", controlspec.new(0, 1, "lin", 0.01, 0.5))
    params:set_action("rev_decay", function(v) engine.revDecay(v) end)
    
    params:add_control("texture", "Texture", controlspec.new(0, 1, "lin", 0.01, 0.5))
    params:set_action("texture", function(v) engine.texture(v) end)
    
    params:add_control("density", "Density", controlspec.new(50, 200, "lin", 1, 100, "ms"))
    params:set_action("density", function(v) engine.density(v) end)
    
    params:add_control("rev_mix", "Reverb Mix", controlspec.new(0, 1, "lin", 0.01, 0.3))
    params:set_action("rev_mix", function(v) engine.revMix(v) end)
    
    params:add_separator("rongs_mod", "MODULATION")
    
    params:add_control("am_depth", "AM Depth", controlspec.new(0, 1, "lin", 0.01, 0))
    params:set_action("am_depth", function(v) engine.amDepth(v) end)
    
    params:add_control("fm_depth", "FM Depth", controlspec.new(0, 1, "lin", 0.01, 0))
    params:set_action("fm_depth", function(v) engine.fmDepth(v) end)
    
    params:add_separator("rongs_io", "INPUT/OUTPUT")
    
    params:add_control("ext_in_level", "Ext Input Level", controlspec.new(0, 1, "lin", 0.01, 0))
    params:set_action("ext_in_level", function(v) engine.inputLevel(v) end)
    
    params:add_control("master_level", "Master Level", controlspec.new(0, 1, "lin", 0.01, 0.7))
    params:set_action("master_level", function(v) engine.masterLevel(v) end)
    
    params:add_separator("rongs_midi", "MIDI")
    
    params:add_number("midi_channel", "MIDI Channel", 0, 16, 0)  -- 0 = omni
    
    -- Wait for engine to initialize before banging params
    clock.run(function()
        clock.sleep(0.5)
        params:bang()
    end)
    
    -- Redraw timer
    clock.run(function()
        while true do
            clock.sleep(1/15)
            redraw()
            grid_redraw()
            arc_redraw()
        end
    end)
end

-----------------------
-- GRID
-----------------------
function g.key(x, y, z)
    -- Keyboard area (rows 1-6, bottom to top)
    if y <= KEY_ROWS then
        local note = grid_to_note(x, y)
        if z == 1 then
            note_on(note, 1.0)
            held_keys[x .. "," .. y] = note
        else
            local held_note = held_keys[x .. "," .. y]
            if held_note then
                note_off(held_note)
                held_keys[x .. "," .. y] = nil
            end
        end
    
    -- Row 7: Mode and controls
    elseif y == 7 then
        if z == 1 then
            -- Cols 1-3: Mode select
            if x >= 1 and x <= 3 then
                params:set("mode", x)
            -- Cols 5-6: Arc page
            elseif x == 5 then
                arc_page = 1
            elseif x == 6 then
                arc_page = 2
            -- Cols 12-16: Freeze
            elseif x >= 12 and x <= 16 then
                set_freeze(1)
            end
        else
            -- Release freeze
            if x >= 12 and x <= 16 then
                set_freeze(0)
            end
        end
    
    -- Row 8: Presets
    elseif y == 8 then
        if z == 1 then
            -- Cols 1-4: Lo-fi presets
            if x >= 1 and x <= 4 then
                local preset = lofi_presets[x]
                params:set("bit_depth", preset.bits)
                params:set("sample_rate", preset.sr)
            -- Cols 6-8: Feedback presets
            elseif x >= 6 and x <= 8 then
                params:set("feedback", feedback_presets[x - 5])
            -- Cols 10-12: AM presets
            elseif x >= 10 and x <= 12 then
                params:set("am_depth", am_presets[x - 9])
            -- Cols 14-16: FM presets
            elseif x >= 14 and x <= 16 then
                params:set("fm_depth", fm_presets[x - 13])
            end
        end
    end
end

function grid_to_note(x, y)
    -- Bottom row (y=6) starts at BASE_NOTE
    -- Each row up adds 16 semitones (full row)
    local row_offset = (KEY_ROWS - y) * GRID_COLS
    return BASE_NOTE + row_offset + (x - 1)
end

function grid_redraw()
    if not g.device then return end
    g:all(0)
    
    -- Keyboard area - show white/black keys
    -- White keys: C, D, E, F, G, A, B (0, 2, 4, 5, 7, 9, 11)
    -- Black keys: C#, D#, F#, G#, A# (1, 3, 6, 8, 10)
    local white_notes = {[0]=true, [2]=true, [4]=true, [5]=true, [7]=true, [9]=true, [11]=true}
    
    for y = 1, KEY_ROWS do
        for x = 1, GRID_COLS do
            local note = grid_to_note(x, y)
            local note_class = note % 12
            local brightness
            
            if held_keys[x .. "," .. y] then
                -- Held note = brightest
                brightness = 15
            elseif white_notes[note_class] then
                -- White key
                if note_class == 0 then
                    -- C notes slightly brighter
                    brightness = 8
                else
                    brightness = 5
                end
            else
                -- Black key
                brightness = 2
            end
            g:led(x, y, brightness)
        end
    end
    
    -- Row 7: Mode and controls
    for x = 1, 3 do
        g:led(x, 7, x == mode and 15 or 4)
    end
    g:led(5, 7, arc_page == 1 and 12 or 4)
    g:led(6, 7, arc_page == 2 and 12 or 4)
    for x = 12, 16 do
        g:led(x, 7, freeze_state == 1 and 15 or 4)
    end
    
    -- Row 8: Presets (static indicators)
    for x = 1, 4 do g:led(x, 8, 4) end
    for x = 6, 8 do g:led(x, 8, 4) end
    for x = 10, 12 do g:led(x, 8, 4) end
    for x = 14, 16 do g:led(x, 8, 4) end
    
    g:refresh()
end

-----------------------
-- ARC
-----------------------
function a.delta(n, d)
    if not arc_params[arc_page] then return end
    local param_name = arc_params[arc_page][n]
    if param_name then
        params:delta(param_name, d / 10)
    end
end

function arc_redraw()
    if not a.device then return end
    a:all(0)
    
    for i = 1, 4 do
        local param_name = arc_params[arc_page][i]
        if param_name then
            local val = params:get(param_name)
            local max = params:get_range(param_name)[2]
            local min = params:get_range(param_name)[1]
            local norm = (val - min) / (max - min)
            
            -- Draw arc segment
            local led_count = math.floor(norm * 64)
            for j = 1, led_count do
                a:led(i, j, 12)
            end
            -- Bright indicator at position
            a:led(i, math.max(1, led_count), 15)
        end
    end
    
    a:refresh()
end

-----------------------
-- MIDI
-----------------------
function m.event(data)
    local msg = midi.to_msg(data)
    local ch = params:get("midi_channel")
    
    -- Check channel (0 = omni)
    if ch > 0 and msg.ch ~= ch then return end
    
    if msg.type == "note_on" and msg.vel > 0 then
        note_on(msg.note, msg.vel / 127)
    elseif msg.type == "note_off" or (msg.type == "note_on" and msg.vel == 0) then
        note_off(msg.note)
    end
end

-----------------------
-- NORNS CONTROLS
-----------------------
function key(n, z)
    if n == 1 then
        shift = z == 1
    elseif n == 2 and z == 1 then
        arc_page = arc_page == 1 and 2 or 1
    elseif n == 3 then
        set_freeze(z)
    end
end

function enc(n, d)
    if n == 1 then
        params:delta("master_level", d)
    elseif n == 2 then
        if shift then
            params:delta("ext_in_level", d)
        else
            params:delta("delay_mix", d)
        end
    elseif n == 3 then
        if shift then
            params:delta("feedback", d)
        else
            params:delta("density", d)
        end
    end
end

-----------------------
-- SCREEN
-----------------------
function redraw()
    screen.clear()
    screen.level(15)
    
    -- Title
    screen.move(64, 8)
    screen.text_center("RONGS")
    
    -- Mode
    screen.level(8)
    screen.move(4, 20)
    screen.text("Mode: ")
    screen.level(15)
    screen.text(mode_names[mode])
    
    -- Arc page
    screen.level(8)
    screen.move(4, 30)
    screen.text("Arc: ")
    screen.level(15)
    screen.text("Page " .. arc_page)
    
    -- Delay size
    local delay_sizes = {"Small", "Medium", "Large"}
    screen.level(8)
    screen.move(70, 20)
    screen.text("Dly: ")
    screen.level(15)
    screen.text(delay_sizes[params:get("delay_size")])
    
    -- Freeze indicator
    if freeze_state == 1 then
        screen.level(15)
        screen.move(70, 30)
        screen.text("FREEZE")
    end
    
    -- Current Arc page params
    screen.level(8)
    screen.move(4, 45)
    local params_list = arc_params[arc_page]
    for i, p in ipairs(params_list) do
        local val = params:get(p)
        screen.move(4 + (i-1) * 32, 45)
        screen.level(4)
        screen.text(string.sub(p, 1, 4))
        screen.move(4 + (i-1) * 32, 55)
        screen.level(12)
        screen.text(string.format("%.2f", val))
    end
    
    -- Input level indicator
    local inp = params:get("ext_in_level")
    if inp > 0 then
        screen.level(8)
        screen.move(4, 64)
        screen.text("IN: ")
        screen.level(15)
        screen.rect(24, 58, inp * 40, 6)
        screen.fill()
    end
    
    screen.update()
end

-----------------------
-- HELPERS
-----------------------
function note_on(note, vel)
    engine.noteOn(note, vel)
end

function note_off(note)
    engine.noteOff(note)
end

function set_freeze(state)
    freeze_state = state
    engine.freeze(state)
end

function cleanup()
    -- Release all notes
    for k, note in pairs(held_keys) do
        note_off(note)
    end
end
