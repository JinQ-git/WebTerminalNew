# Web Terminal

## Overview

![overview](https://github.com/JinQ-git/WebTerminal/blob/master/doc/overview.png)

## Build Projects

Use `maven`

## Expose WebTerminal

Never expose it directly without some of encryption. This project do not use secure protocol. So, it is vulnerable to `man in the middle attack`.

There are several approaches to exposing WebTerminal securely:

- Upgrade protocol to `https` with your own certificate (Recommend [`Let's Encrypt`](https://letsencrypt.org/) for free certicate)
- or using [`Let's Encrypt`](https://letsencrypt.org/) with NGINX (using reverse proxy)

## How to use NGINX

working in progres...
