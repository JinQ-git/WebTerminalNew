# Web Terminal Protocol Specification

This document describes the my simple web terminal protocol between browser and websocket server.

## Abstract

Both browser and server must treate websocket message like below:
   * *case* **binary**: protocol message that is described in current document.
   * *case* **text**: terminal I/O data.

The protocol message uses [JSON-RPC](https://www.jsonrpc.org) to describe requests, responses and notifications.
The message must be `utf-8` encoded **binary** data.

## Protocol Message

The following TypeScript definitions describe the base [JSON-RPC protocol](https://www.jsonrpc.org):

### Base Types

The protocol use the following definitions for integers, unsigned integers, decimal numbers, objects and arrays:

```ts
/**
 * Defines an integer number in the range of -2^31 to 2^31 - 1.
 */
export type integer = number;
```

```ts
/**
 * The Prompt Type
 */
export namespace PromptType {
    export const BooleanType: string = "boolean";
    export const StringType: string = "string";
    export const PasswordType: string = "password";
}
```

### Connect Request

The connect request is sent as the first request from the client to the server. If the server receives a request or notification before the `connect` request it should act as follows:

- For a request the response should be an error with code: `-32002`. The message can be picked by the server.
- Notifications should be dropped.

Until the server has responded to the `connect` request with an `ConnectParams`, the client must not send any additional requests or notifications to the server except `prompt` request.

The `connect` request may only be sent once per terminal.

*Request:*

- method: 'connect'

- params: `ConnectParams` defined as follows:

   ```ts
   interface ConnectParams {
       /**
        * The terminal Id of the target client terminal
        */
       terminalId: string;
   
       /**
        * Username
        */
       user: string;
   
       /**
        * The host name of SSH server.
        * If omitted, try connect to `localhost`
        */
       host?: string;
   
       /**
        * The port number of SSH server.
        * If omitted, use default port number 22.
        */
       port?: integer;
   
       /**
        * If omitted, use true
        */
       strictHostCheck?: boolean;

       /**
        * The timeout in millisecond.
        * If omitted, use default value 5000 (5sec)
        */
       timeout?: integer;
   
       /**
        * The charset name that ssh server is used for encoding.
        * Web client only supports utf-8 encoded string, so Websocket server must perform charset conversion.
        *  ] SSH Server -=[encoding string]=-> WebScoket Server -=[utf-8 string]=-> Client [
        * If omitted, use `utf-8`
        */
       encoding?: string;
   
       /**
        * The terminal size
        */
       size?: {
           cols: integer;
           rows: integer;
       };
   }
   ```

*Response:*

- result: `true`

- error.code: 

   ```ts
   export namespace ConnectErrorCodes {
       export const UnknownHost: integer = 1;
       export const UnknownUser: integer = 2;
       export const AuthenticationFailed: integer = 3;
       export const UserCancel: integer = 3;
   }
   ```

### Prompt Request

The prompt request is sent from server to client while the server try connecting to the SSH server.
This request is for asking `Host Key Check`, `Password`, `Multi-factor authentication` and so on.

*Request:*

- method: 'prompt'

- params: `PromptParams` defined as follows:

   ```ts
   interface PromptParams {
       /**
        * The terminal Id that is specified in the connect request
        */
       terminalId: string;
   
       /**
        * The prompt message
        */
       message: string;
   
       /**
        * The prompt type 
        *     - BooleanType: Prompt Yes or No
        *     - StringType: (Default)
        *     - PasswordType: Prompt Password or Passphrase
        * If omitted, default value is `StringType`
        */
       type?: PromptType;
   }
   ```

*Response:*

- result: `string | null`

   > NOTE: use **'Y'** or **'N'** in case response of the `BooleanType`.

   > NOTE: If `null` is specified, the server treated this response as cancel the prompt. 
   In that case, the server should response `UserCancel` error for the connect request.

### Resize Terminal Notification

The resize terminal notification is sent from client to server. This notification can be sent at any time after complete the connect request.

*Notification*

- method: 'resizeTerminal'

- params: `ResizeParams` defined as follows:

   ```ts
   interface ResizeParams {
       /**
        * The terminal Id
        */
       terminalId: string;
   
       /**
        * The terminal size: columns
        */
       cols: integer;

       /**
        * The terminal size: rows
        */
       rows: integer;
   }
   ```

### Change Encoding Notification

The change encoding notification is sent from client to server. This notification can be sent at any time after complete the connect request.

*Notification*

- method: 'changeEncoding'

- params: `EncodingParams` defined as follows:

   ```ts
   interface EncodingParams {
       /**
        * The terminal Id
        */
       terminalId: string;
   
       /**
        * The charset name that ssh server is used for encoding.
        * Web client only supports utf-8 encoded string, so Websocket server must perform charset conversion.
        *  ] SSH Server -=[encoding string]=-> WebScoket Server -=[utf-8 string]=-> Client [
        * If omitted, use `utf-8`
        */
       encoding?: string;
   }
   ```

