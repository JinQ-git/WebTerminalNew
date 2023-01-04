let ASSIST_MODE_ON = false;
let ASSIST_MODE_FLAG = 0;

function handleAssistantKeyDown(event) { handleAssistantKeyDownImpl(event.key); }

function toggleAssistantButtons() {
    ASSIST_MODE_ON = document.body.classList.toggle("assistant");
    document.getElementById("BTN_CTRL").classList.remove("btn-dark");
    document.getElementById("BTN_FN").classList.remove("btn-dark");
    ASSIST_MODE_FLAG = 0;

    // ** NOTE: Follow code is not working on android virtual keyboard **
    // if(ASSIST_MODE_ON ) {
    //     window.addEventListener("keydown", handleAssistantKeyDown, true);
    // }
    // else {
    //     window.removeEventListener("keydown", handleAssistantKeyDown, true);
    // }

    resizeViewport();
}

function onAssistEscape()
{
    if( ASSIST_MODE_ON ) {
        let terminal = getCurrentTerminal();
        _sendData( terminal, TerminalKeyCodes.Escape );
    }
}

function onAssistTab()
{
    if( ASSIST_MODE_ON ) {
        let terminal = getCurrentTerminal();
        _sendData( terminal, TerminalKeyCodes.Tab );
    }
}

function onAssistCtrl()
{
    if( ASSIST_MODE_ON ) {
        if( document.getElementById("BTN_CTRL").classList.toggle("btn-dark") ) {
            document.getElementById("BTN_FN").classList.remove("btn-dark");
            ASSIST_MODE_FLAG = 1;
        }
        else {
            ASSIST_MODE_FLAG = 0;
        }

        let terminal = getCurrentTerminal();
        terminal.focus();
    }
}

function onAssistFn()
{
    if( document.getElementById("BTN_FN").classList.toggle("btn-dark") ) {
        document.getElementById("BTN_CTRL").classList.remove("btn-dark");
        ASSIST_MODE_FLAG = 2;
    }
    else {
        ASSIST_MODE_FLAG = 0;
    }

    let terminal = getCurrentTerminal();
    terminal.focus();
}

function onAssistLeftArrow()
{
    if( ASSIST_MODE_ON ) {
        let terminal = getCurrentTerminal();
        _sendData( terminal, TerminalKeyCodes.ArrowLeft );
    }
}

function onAssistRightArrow()
{
    if( ASSIST_MODE_ON ) {
        let terminal = getCurrentTerminal();
        _sendData( terminal, TerminalKeyCodes.ArrowRight );
    }
}

function onAssistDownArrow()
{
    if( ASSIST_MODE_ON ) {
        let terminal = getCurrentTerminal();
        _sendData( terminal, TerminalKeyCodes.ArrowDown );
    }
}

function onAssistUpArrow()
{
    if( ASSIST_MODE_ON ) {
        let terminal = getCurrentTerminal();
        _sendData( terminal, TerminalKeyCodes.ArrowUp );
    }
}

function handleAssistantKeyDownImpl(keyVal, websocket)
{
    if( ASSIST_MODE_ON && ASSIST_MODE_FLAG > 0 ) 
    {
        let spKey = false;
        if( keyVal.length == 1 ) // Only for Single Character
        {
            if( ASSIST_MODE_FLAG == 1 )
            {
                let idx = TerminalKeyCodes.CTRL_CHAR.indexOf(keyVal);
                if( idx >= 0 ) { spKey = TerminalKeyCodes.CTRL_LIST[idx]; }
            }
            else if( ASSIST_MODE_FLAG == 2 )
            {
                let idx = TerminalKeyCodes.FN_CHAR.indexOf(keyVal);
                if( idx >= 0 ) { spKey = TerminalKeyCodes.FN_LIST[idx]; }
            }
        }

        // Consume any key
        document.getElementById("BTN_CTRL").classList.remove("btn-dark");
        document.getElementById("BTN_FN").classList.remove("btn-dark");
        ASSIST_MODE_FLAG = 0;

        if( spKey !== false ) {
            if( websocket instanceof WebSocket) {
                websocket.send(spKey);
            }
            else {
                let terminal = getCurrentTerminal();
                _sendData(terminal, spKey);
            }
            return true;
        }
    }
    return false;
}
