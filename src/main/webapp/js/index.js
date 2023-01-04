const CMD = new MyCommand(true);
const TERM = new Terminal();
const RESIZE_DELAY = 500;
const DEFAULT_PADDING_H = 2;
const DEFAULT_PADDING_V = 0;
const CE_OPT = { code: 1001, reason: "Unexpected client condition" /*, wasClean: true*/ };

const BEFORE_UNLOAD_LISTENER = (event) => {
    event.preventDefault();
    return event.returnValue = "You will be lose current connected session, are you sure?";
};

let RESIZE_TIMEOUT = false;

/*
 * [ xterm.js - android virtual keyboard problem ]
 *
 * Those whom use android device and its virtual keyboard are strongly recommended that
 * turn off any smart keyboard features that use buffered input like ["Predictive text" (samsung), "Text Correction" (android keyboard), "Auto-Correction" and so on].
 * Eventhough you turned off smart keyboard feature, there may still be some problem, for example, android keyboard has backspace problem. (buffered text pop out)
 * In that case, strongly recommand use physical keyboard instead of virtual keyboard on android device.
 * 
 * Unfortunately, android virtual keyboard does not fire "Key" event of xterm.js and window object.
 */

/*
 * [ iOS bug - in case use physical keyboard (include smart folio) ]
 *
 * When you press [Ctrl + C] webkit fires KeyboardEvent, and its `which` and `keyCode` properties is set by 13 <- generally it must be 67
 * most of case `which` and `keyCode` is deprecated properties so no problems, but xtermjs seems to be using those values.
 * !!Not only [Ctrl + C] but also [Ctrl + H] -> for [Ctrl + H] `which` value is 8 <- generally it must be 72
 * 
 * Note that only iOS webkit based browsers (safari, chrome, edge) have this bug!!
 * Note!! not only smart folio keyboard but also any physical (usb) keyboard!!!
 */

function isSecuredProtocol() { return (location.protocol == "https:"); }
function getAppNameFromPathName() { // for tomcat URL
    let pathName = location.pathname; // begin with '/'
    let found = pathName.indexOf( '/', 1 );

    return found < 0 ? pathName : pathName.substring(0, found);
}
function getParentPathName() {
    let pathName = location.pathname; // begin with '/'
    let found = pathName.lastIndexOf('/');

    return found > 0 ? pathName.substring(0, found) : pathName;
}
function getWebScoketAddress() {
    let protocol = isSecuredProtocol() ? "wss://" : "ws://";
    let host     = location.host; // include port (if exist)
    let basePathName  = getAppNameFromPathName(); // containing an initial '/'

    return protocol + host + basePathName + "/ws/ssh";
}

function initializeGUI()
{
    let connDlgElement = document.getElementById("ConnectDialog");
    let promptDlgElement = document.getElementById("PromptDialog");

    // Event for PromptDialog
    promptDlgElement.addEventListener("shown.bs.modal", function(event){
        if( PromptDialog.hasOwnProperty("focusTarget") && PromptDialog.focusTarget instanceof HTMLElement ) {
            PromptDialog.focusTarget.focus();
        }
    });

    promptDlgElement.addEventListener("hide.bs.modal", function(event) {
        if( PromptDialog.hasOwnProperty("msgId") && PromptDialog.hasOwnProperty("targetWS") )
        {
            if( PromptDialog.targetWS instanceof WebSocket )
            {
                let respMsg = typeof PromptDialog.responseMessage === "string" 
                            ? PromptDialog.responseMessage
                            : null;

                let msg = CMD.encodeBinary( CMD.response(PromptDialog.msgId, respMsg) );
                PromptDialog.targetWS.send(msg);
            }
        }

        delete PromptDialog.focusTarget;
        delete PromptDialog.msgId;
        delete PromptDialog.targetWS;
        delete PromptDialog.responseMessage;
    });

    document.getElementById("PROMPT_IN").addEventListener("keydown", function(event) {
        let handeled = false;
        if( event.key !== undefined ) {
            handeled = (event.key === "Enter");
        }
        else if( event.keyCode !== undefined ) {
            handeled = (event.keyCode === 13);
        }

        if( handeled ) {
            event.preventDefault();
            onClickConfirmPrompt();
        }
    }, true);

    // Define BootStrap Dialogs
    Object.defineProperties(window, {
        ConnectDialog: {
            value: new bootstrap.Modal(connDlgElement)
        },
        PromptDialog: {
            value: new bootstrap.Modal(promptDlgElement)
        }
    });

    // Open SignIn Dialog
    ConnectDialog.show();
}

function initializeTerminal(terminal)
{
    if( terminal instanceof Terminal )
    {
        if( !terminal.hasOwnProperty("id") ) {
            terminal.id = String("TM_" + Date.now());
        }

        let fitAddon = new FitAddon.FitAddon();
        let terminalDOM = document.getElementById('terminal'); // terminal.id

        terminal.fitAddon = fitAddon;
        terminal.loadAddon(fitAddon);

        terminal.open(terminalDOM);
        onResizeTerminal(terminal);
        
        terminal.disposer = {};
        terminal.addEvent = function(which, fn) {
            if( typeof which === "string" && typeof fn === "function" )
            {
                switch(which)
                {
                    case "key":
                        terminal.disposer[which] = terminal.onKey(fn);
                        break;

                    case "data":
                        terminal.disposer[which] = terminal.onData(fn);
                        break;

                    case "resize":
                        terminal.disposer[which] = terminal.onResize(fn);
                        break;
                }
            }
        }
        terminal.removeEvent = function(which) {
            if( typeof which === "string" ) {
                if( terminal.disposer[which] ){
                    let disposer = terminal.disposer[which]
                    delete terminal.disposer[which];
                    disposer.dispose();
                }
            }
        }
        terminal.removeEvents = function() {
            if( typeof terminal.disposer === "object" ) {
                for( which in terminal.disposer ) {
                    let disposer = terminal.disposer[which];
                    delete terminal.disposer[which];
                    try { disposer.dispose(); }catch(e){}
                }
            }
        }
        terminal.resetAll = function() {
            terminal.removeEvents();
            terminal.reset();
        }
    }
}

function getCurrentTerminal() {
    return TERM;
}

function _sendData(terminal, text)
{
    if( terminal instanceof Terminal && 
        terminal.currentWS instanceof WebSocket &&
        terminal.currentWS.readyState == 1 &&
        typeof text === "string" )
    {
        terminal.currentWS.send(text);
    }
    terminal.focus();
}

function onResizeTerminal(terminal) {
    if( terminal instanceof Terminal )
    {
        if( terminal.fitAddon instanceof FitAddon.FitAddon ) 
        {
            let termContainer = document.getElementById("TERMINAL_CONTAINER");

            // Reset Terminal Container's Padding
            termContainer.style.paddingLeft = DEFAULT_PADDING_H + "px";
            termContainer.style.paddingTop  = DEFAULT_PADDING_V + "px";

            // Resize Terminal
            terminal.fitAddon.fit();
            terminal.scrollLines(-1);
            terminal.scrollLines(1);            

            // Calculate Margin
            let core = terminal._core;
            let scrollBarWidth = core.viewport.scrollBarWidth;
            let xscreenE = core.screenElement;

            let parentE = terminal.element.parentElement;

            let dx = parentE.clientWidth - xscreenE.clientWidth - scrollBarWidth;
            let dy = parentE.clientHeight - xscreenE.clientHeight;

            if( dx > 0 ) {
                let paddingLeft = DEFAULT_PADDING_H + Math.ceil(dx / 2);
                termContainer.style.paddingLeft = paddingLeft + "px";
            }

            if( dy > 0 ) {
                let paddingTop = DEFAULT_PADDING_V + Math.ceil(dy / 2);
                termContainer.style.paddingTop = paddingTop + "px";
            }
        }
    }
}

function linkTerminalAndWebsocket(terminal, websocket)
{
    if( terminal instanceof Terminal && websocket instanceof WebSocket )
    {
        terminal.currentWS = websocket;

        terminal.removeEvents();
        terminal.addEvent("data", function(data) {
            if(!handleAssistantKeyDownImpl(data, websocket)) {
                websocket.send(data);
            }
        });
        terminal.addEvent("resize", function() {
            // let dims = terminal._core._renderService.dimensions;
            // let terminalPixelWidth  = dims.actualCellWidth  * terminal.cols;
            // let terminalPixelHeight = dims.actualCellHeight * terminal.rows;

            let msg = CMD.encodeBinary( CMD.resizeNotify(terminal.id, terminal.cols, terminal.rows) );
            websocket.send(msg);
        });

        websocket.onmessage = function(event) {
            if( typeof event.data === "string" ) {
                terminal.write(event.data);
            }
        }
    }
}

// Rough Sketch
const handleMessageWhileConnect = function(event) {
    // Websocket onmessage

    // Only binary message
    if( event.data instanceof ArrayBuffer ) {
        // Parse JsonRPC -> event.data should be utf-8 encoded json string
        let msg = JSON.parse( CMD.decodeBinary(event.data) );
        if( msg.jsonrpc === "2.0" ) {
            let currentWS = event.currentTarget;
            if( msg.hasOwnProperty("method") ) // Request from server
            {
                let err = null;

                if( msg.method === "prompt" )
                {
                    let promptParams = msg.params;
                    // Check Terminal Id
                    if( promptParams.terminalId === currentWS.targetTerminal.id )
                    {
                        // Show Dialog
                        showPrompt(msg.id, promptParams, currentWS);
                        return;
                    }
                    else { // Invalid Request!! (Wrong Terminal ID)
                        err = CMD.error( msg.id, ErrorCodes.InvalidParams, "Unknown Terminal Id: " + promptParams.terminalId );
                    }
                }
                else { // Invalid Request!! (Unsupport method)
                    err = CMD.error( msg.id, ErrorCodes.MethodNotFound, "Not support method: " + msg.method )
                }

                if( err ) {
                    // Send Error Response
                    currentWS.send( CMD.encodeBinary(err) );
                }
            }
            else // Response from server
            {
                // Check Id
                if( currentWS.connectId === msg.id ) 
                {
                    if( msg.hasOwnProperty("result")) // Connection Success
                    {
                        if( currentWS.targetTerminal instanceof Terminal ) {
                            linkTerminalAndWebsocket( currentWS.targetTerminal, currentWS );
                            currentWS.targetTerminal.focus();
                        }
                        else {
                            currentWS.close(CE_OPT.code, CE_OPT.reason);
                            currentWS.dispatchEvent(new CloseEvent("close", CE_OPT));
                        }
                    }
                    else if( msg.hasOwnProperty("error")) // Connection Failed
                    {
                        let errMsg = "Connection Failed: " + msg.error.message;
                        if( currentWS.targetTerminal instanceof Terminal ) {
                            currentWS.targetTerminal.writeln(errMsg);
                        }
                        else {
                            alert(errMsg);
                        }
                    }
                }
            }
        }
    }
    else if( typeof event.data === "string") {
        let currentWS = event.currentTarget;
        if( currentWS.targetTerminal instanceof Terminal ) {
            currentWS.targetTerminal.writeln(event.data);
        }
    }
}

function onClickConnect()
{
    let user = document.getElementById("CONN_USER_NAME").value.trim();
    let host = document.getElementById("CONN_HOST").value.trim();
    let port = Number(document.getElementById("CONN_PORT").value.trim());
    let shc  = document.getElementById("CONN_SHC").checked;

    if( user.length === 0 ) {
        alert("Please input Username");
        document.getElementById("CONN_USER_NAME").focus();
        return;
    }

    let opt = {};
    if( host.length > 0 ) { opt.host = host; }
    if( !isNaN(port) && port > 0 && port < 65536 ) { opt.port = port; }
    if(!shc) { opt.strictHostCheck = shc; }

    // Current Terminal
    let terminal = TERM; // or create, open multiple terminal on single view
    onSignIn( terminal, user, opt );

    ConnectDialog.hide();
}

function onSignIn(terminal, username, options)
{
    //if( !( terminal instanceof Terminal ) ) { return; }
    //if( !(terminal.hasOwnProperty("id")) ) { return; }

    let url = getWebScoketAddress();
    let ws = new WebSocket(url);

    ws.binaryType     = "arraybuffer";
    ws.connectId      = Date.now(); // use timestamp as connection Id
    ws.targetTerminal = terminal;

    if( !options.hasOwnProperty("cols") || typeof options.cols !== "number" || 
        !options.hasOwnProperty("rows") || typeof options.rows !== "number"  ) {
        options.cols = terminal.cols;
        options.rows = terminal.rows;
    }

    ws.onopen = function(event) {
        window.addEventListener("beforeunload", BEFORE_UNLOAD_LISTENER);
        window.addEventListener("keydown", handleCtrlKey_ios, true);

        let currentWS = event.currentTarget;
        if( !currentWS.hasOwnProperty("connectId") ) {
            currentWS.close(CE_OPT.code, CE_OPT.reason);
            currentWS.dispatchEvent(new CloseEvent("close", CE_OPT));
            return;
        }
        terminal.reset();

        let connMsg = CMD.connect(
            currentWS.connectId, 
            terminal.id, 
            username,
            options 
        );

        ws.send( CMD.encodeBinary(connMsg) );
    };
    ws.onmessage = handleMessageWhileConnect;
    ws.onclose = function(event) {
        console.log("Websocket has been closed with code: " + event.code + "\n" + event.reason );

        window.removeEventListener("beforeunload", BEFORE_UNLOAD_LISTENER);
        window.removeEventListener("keydown", handleCtrlKey_ios, true);

        // closed from server
        let currentWS = event.currentTarget;
        if( currentWS.targetTerminal instanceof Terminal ) {
            currentWS.targetTerminal.currentWS = null; // unbind websocket
            currentWS.targetTerminal.removeEvents();

            currentWS.targetTerminal.writeln("");
            if( event.code != 1000 ) {
                currentWS.targetTerminal.writeln( "Websocket closed with code: " + event.code );
            }
            if( event.reason.length > 0 ) {
                currentWS.targetTerminal.writeln( event.reason );
            }
        }

        ConnectDialog.show();
    }
}

function showPrompt(msgId, params, targetWS)
{
    if( targetWS instanceof WebSocket && 
        typeof params === "object" && typeof params.message === "string" )
    {
        document.getElementById("PROMPT_MSG").innerText = params.message;

        let inputElement = document.getElementById("PROMPT_IN");
        inputElement.value = "";

        let type = ( typeof params.type === "string" ) ? params.type : "string";
        if(type === "boolean")
        {
            inputElement.type = "text";
            document.getElementById("PROMPT_INPUT_AREA").classList.add("d-none");
            document.getElementById("PROMPT_CONFIRM").classList.add("d-none");
            document.getElementById("PROMPT_YES_NO").classList.remove("d-none");

            PromptDialog.focusTarget = document.querySelector("#PROMPT_YES_NO > .btn-primary");
        }
        else
        {
            inputElement.type = type === "password" ? "password" : "text";
            document.getElementById("PROMPT_INPUT_AREA").classList.remove("d-none");
            document.getElementById("PROMPT_CONFIRM").classList.remove("d-none");
            document.getElementById("PROMPT_YES_NO").classList.add("d-none");

            PromptDialog.focusTarget = inputElement;
        }

        PromptDialog.msgId = msgId;
        PromptDialog.targetWS = targetWS;
        PromptDialog.responseMessage = null;
        PromptDialog.show();
    }
}

function onClickConfirmPrompt()
{
    let promptResp = document.getElementById("PROMPT_IN").value.trim();
    if(promptResp.length === 0) {
        alert("Please input your answer");
        let inputElement = document.getElementById("PROMPT_IN");
        inputElement.value = ""; // Remove space (if exist)
        inputElement.focus();
        return;
    }

    if( PromptDialog.hasOwnProperty("msgId") && PromptDialog.hasOwnProperty("targetWS") )
    {
        PromptDialog.responseMessage = promptResp;
    }

    PromptDialog.hide();
}

function onClickYesNoPrompt(isYes)
{
    let promptResp = Boolean(isYes) ? "Y" : "N";

    if( PromptDialog.hasOwnProperty("msgId") && PromptDialog.hasOwnProperty("targetWS") )
    {
        PromptDialog.responseMessage = promptResp;
    }

    PromptDialog.hide();
}

function resizeViewport()
{
    let termDom = document.getElementById("TERMINAL_CONTAINER");
    if(window.visualViewport) {
        let vp = window.visualViewport;
        let termHeight = vp.height;
        if( document.body.classList.contains("assistant") ) {
            let assistDom = document.getElementById("AssistantKeyList");
            let assistHeight = assistDom.getBoundingClientRect().height;

            termHeight -= assistHeight;

            let offsetBottom = window.innerHeight - vp.height;
            if( offsetBottom < 0 ) { offsetBottom = 0; }
            assistDom.style.bottom = offsetBottom + "px";
        }
        termDom.style.height = termHeight + "px";
    }
    else {
        if( document.body.classList.contains("assistant") ) {
            let assistDom = document.getElementById("AssistantKeyList");
            let assistHeight = assistDom.getBoundingClientRect().height;

            termDom.style.paddingBottom = assistHeight + "px";
        }
        else {
            termDom.style.paddingBottom = null;
        }
    }

    onResizeTerminal(TERM);
}

function handleCtrlKey_ios(event) // catch "keydown" event
{
    if( event instanceof KeyboardEvent )
    {
        if(event.ctrlKey && event.key.length == 1)
        {
            if( !event.altKey && !event.shiftKey && !event.metaKey )
            {
                let keyVal = event.key.toLowerCase();
                let idx = TerminalKeyCodes.CTRL_CHAR.indexOf(keyVal);
                if( idx >= 0 ) { 
                    let spKey = TerminalKeyCodes.CTRL_LIST[idx];
                    event.preventDefault();
                    event.stopPropagation();

                    let terminal = getCurrentTerminal();
                    _sendData(terminal, spKey);
                }
            }
        }
    }
}

window.onload = function()
{
    // Initialize JQueryUI
    initializeGUI();

    // Ready Terminal
    initializeTerminal(TERM);

    const resizeFn = function(ev) {
        clearTimeout( RESIZE_TIMEOUT );
        RESIZE_TIMEOUT = setTimeout(function() {
            resizeViewport();
        }, RESIZE_DELAY);
    };

    if(window.visualViewport) {
        window.visualViewport.onresize = resizeFn;
    }
    else {
        window.onresize = resizeFn;
    }

    window.addEventListener("touchstart", function(event) {
        event.preventDefault();
        event.stopPropagation();

        if( event.touches.length == 2 ) {
            toggleAssistantButtons(); // mobileAssist.js
        }
    });
}