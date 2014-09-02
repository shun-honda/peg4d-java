#!/bin/bash

JAR="peg4d.jar"
LOG="result_vm_test.txt"
COUNT=0
OK=0

echo > $LOG
for a in `ls ./test/*.peg`  ; do
	echo "test rule: ${a}"
	t=${a/\.peg/_}
	for b in `ls ${t}* ` ; do
		COUNT=$((COUNT + 1))
		echo "run test ${b}"
		java -jar ${JAR} -p ${a} -f vm ${b}
		RET=$?
		PREFIX="[failed]"
		if [ $RET == 0 ] ; then
			PREFIX="[sucess]"
			OK=$((OK + 1))
		fi
		msg="${PREFIX}: ${b}"
		echo $msg
		echo $msg >> $LOG
	done
done

echo "test result [${OK}/${COUNT}]" >> $LOG
