# Web Terminal

## Overview

![overview](https://github.com/JinQ-git/WebTerminal/blob/master/doc/overview.png)

## Build Projects

Use `maven`

## New Feature Compared to [old Version](https://github.com/JinQ-git/WebTerminal)

- remove encryption message
- append "keyboard-interactive authentication" feature (now, supports multi-factor authentication such as OTP)

## Expose WebTerminal

Never expose it directly without some of encryption. This project do not use secure protocol. So, it is vulnerable to `man in the middle attack`.

There are several approaches to exposing WebTerminal securely:

- Upgrade protocol to `https` with your own certificate (Recommend [`Let's Encrypt`](https://letsencrypt.org/) for free certicate)
- or using [`Let's Encrypt`](https://letsencrypt.org/) with NGINX (using reverse proxy)

## How to use NGINX

working in progres...
