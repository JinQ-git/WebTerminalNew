const TerminalKeyCodes = {};
Object.defineProperties( TerminalKeyCodes, {
    Escape: { value: "\x1B", writable: false },
    Tab: { value: "\t", writable: false },
    ArrowUp: { value: "\x1B[A", writable: false },
    ArrowDown: { value: "\x1B[B", writable: false },
    ArrowRight: { value: "\x1B[C", writable: false },
    ArrowLeft: { value: "\x1B[D", writable: false },
    F1: { value: "\x1BOP", writable: false },
    F2: { value: "\x1BOQ", writable: false },
    F3: { value: "\x1BOR", writable: false },
    F4: { value: "\x1BOS", writable: false },
    F5: { value: "\x1B[15~", writable: false },
    F6: { value: "\x1B[17~", writable: false },
    F7: { value: "\x1B[18~", writable: false },
    F8: { value: "\x1B[19~", writable: false },
    F9: { value: "\x1B[20~", writable: false },
    F10: { value: "\x1B[21~", writable: false },
    F11: { value: "\x1B[23~", writable: false },
    F12: { value: "\x1B[24~", writable: false },
    Ctrl_A: { value: "\x01", writable: false },
    Ctrl_B: { value: "\x02", writable: false },
    Ctrl_C: { value: "\x03", writable: false },
    Ctrl_D: { value: "\x04", writable: false },
    Ctrl_E: { value: "\x05", writable: false },
    Ctrl_F: { value: "\x06", writable: false },
    Ctrl_G: { value: "\x07", writable: false },
    Ctrl_H: { value: "\x08", writable: false }, // "\b"
    Ctrl_I: { value: "\x09", writable: false }, // "\t"
    Ctrl_J: { value: "\x0A", writable: false }, // "\n"
    Ctrl_K: { value: "\x0B", writable: false }, // "\v"
    Ctrl_L: { value: "\x0C", writable: false }, // "\f"
    Ctrl_M: { value: "\x0D", writable: false }, // "\r"
    Ctrl_N: { value: "\x0E", writable: false },
    Ctrl_O: { value: "\x0F", writable: false },
    Ctrl_P: { value: "\x10", writable: false },
    Ctrl_Q: { value: "\x11", writable: false },
    Ctrl_R: { value: "\x12", writable: false },
    Ctrl_S: { value: "\x13", writable: false },
    Ctrl_T: { value: "\x14", writable: false },
    Ctrl_U: { value: "\x15", writable: false },
    Ctrl_V: { value: "\x16", writable: false },
    Ctrl_W: { value: "\x17", writable: false },
    Ctrl_X: { value: "\x18", writable: false },
    Ctrl_Y: { value: "\x19", writable: false },
    Ctrl_Z: { value: "\x1A", writable: false },
    Ctrl_3: { value: "\x1B", writable: false },
    Ctrl_4: { value: "\x1C", writable: false },
    Ctrl_5: { value: "\x1D", writable: false },
    Ctrl_6: { value: "\x1E", writable: false },
    Ctrl_7: { value: "\x1F", writable: false },
    Ctrl_8: { value: "\x7F", writable: false }, // backspace?

    CTRL_LIST: { value: [ "\x01", "\x02", "\x03", "\x04", "\x05", "\x06", "\x07", "\x08", "\x09", "\x0A", "\x0B", "\x0C", "\x0D", "\x0E", "\x0F", "\x10", "\x11", "\x12", "\x13", "\x14", "\x15", "\x16", "\x17", "\x18", "\x19", "\x1A", "\x1B", "\x1C", "\x1D", "\x1E", "\x1F", "\x7F" ], writable: false },
    FN_LIST: { value: [ "\x1BOP", "\x1BOQ", "\x1BOR", "\x1BOS", "\x1B[15~", "\x1B[17~", "\x1B[18~", "\x1B[19~", "\x1B[20~", "\x1B[21~","\x1B[23~","\x1B[24~" ], writable: false },

    CTRL_CHAR: { value: "abcdefghijklmnopqrstuvwxyz345678", writable: false },
    FN_CHAR: { value: "1234567890()", writable: false }
});