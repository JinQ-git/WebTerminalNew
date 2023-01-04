const ErrorCodes = {
    // Defined by JSON-RPC
	ParseError: -32700,
	InvalidRequest: -32600,
	MethodNotFound: -32601,
	InvalidParams: -32602,
	InternalError: -32603,
};


class MyCommand
{
    constructor(useBinary) {
        this.utf8Encoder = new TextEncoder();
        this.utf8Decoder = new TextDecoder(); // TextDecoder('utf-8');
        this.useBinary = (typeof useBinary === "boolean" && useBinary);
    }

    decodeBinary(data) {
        return this.utf8Decoder.decode(data);
    }

    validateCommand(data) {
        if( data.jsonrpc && data.jsonrpc === "2.0" ) {
            return true;
        }
        return false;
    }

    encodeBinary(jsonObj) {
        let jsonStr = JSON.stringify(jsonObj);
        return this.useBinary ? this.utf8Encoder.encode(jsonStr) : jsonStr;
    }

    connect(id, terminalId, user, options) {
        let params = { terminalId: terminalId, user: user };
        
        if( typeof options === "object" )
        {
            // Validation of the option values must be performed from outside of this method

            if( typeof options.host === "string" ) {
                params.host = options.host;
            }

            if( typeof options.port === "number" ) {
                params.port = options.port;
            }

            if( typeof options.strictHostCheck === "boolean" ) {
                params.strictHostCheck = options.strictHostCheck;
            }

            if( typeof options.encoding === "string" ) {
                params.encoding = options.encoding;
            }

            if( typeof options.rows === "number" && typeof options.cols === "number" ) {
                params.size = { cols: options.cols, rows: options.rows };
            }
        }

        return { jsonrpc: "2.0", id: id, method: "connect", params: params };
    }

    error(id, code, message, data)
    {
        let errMsg = typeof message === "string" ? message : "error";

        let errorObj = {
            jsonrpc: "2.0", 
            id: id, 
            error: {
                code: code,
                message: errMsg
            }
        };

        if( typeof data !== "undefined" ) {
            errorObj.error.data = data;
        }

        return errorObj;
    }

    response(id, result)
    {
        let respObj = {
            jsonrpc: "2.0",
            id: id,
            result: result
        };

        return respObj;
    }

    resizeNotify(tid, cols, rows, wp, hp)
    {
        // based on RFC4254 6.7. Window Dimension Change Message.
        // Params: cols: (required) terminal width, columns
        //         rows: (required) terminal height, rows
        //         wp  : (optional) terminal width, pixels
        //         hp  : (optional) terminal height, pixels
        let notifyObj = {
            jsonrpc: "2.0",
            method: "resizeTerminal",
            params: {
                terminalId: tid,
                cols: cols,
                rows: rows
            }
        };

        if( typeof wp === "number" && typeof hp === "number" ) {
            notifyObj.params.wp = wp;
            notifyObj.params.hp = hp;
        }

        return notifyObj;
    }
}