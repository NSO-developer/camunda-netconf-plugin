#!/bin/bash

encryptKey="2A472D4A614E645267556B5870327335"

echo -n $1 | openssl enc -e -aes-128-ecb -K $encryptKey -a