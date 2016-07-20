#!/bin/bash
#
#  Read Device Address and display it in standard form.
#
#  Ex.  ./read_addr.sh
#
echo $'device nrf51822\n'\
     $'speed 1000\n'\
     $'mem8 100000A4 6\n'\
     $'r\n'\
     $'g\n'\
     $'exit\n'\
     > /tmp/script.jlink

JLinkExe -if SWD -speed 1000 /tmp/script.jlink  > /tmp/output.txt

line="$(grep 100000A4 '/tmp/output.txt')"
prefix="100000A4 = "
bytes=${line#$prefix}

reverse="$(echo ${bytes} | sed 's/\(.* \)\(.* \)\(.* \)\(.* \)\(.* \)\(.*\)/\6 \5\4\3\2\1/g')"
trimmed="$(echo -e "${reverse}" | sed -e 's/[[:space:]]*$//')"

match=' '
replace=':'
dev_addr=${trimmed//$match/$replace}

echo
echo DeviceAddr: ${dev_addr}
echo
