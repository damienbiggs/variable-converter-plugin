# variable-converter-plugin
Helper jenkins plugin from transforming one variable into something else

Specify an existing variable name.
Specify a regular expression that matches the value for that variable.
A new variable can be constructed by using groups from the matched regular expression.

E.g.
For generating deterministc hostnames from an ip address
10.192.168.1
could be transformed to
bos-cambridge-168-1.com
