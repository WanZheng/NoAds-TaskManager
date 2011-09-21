###############################
# device list
###############################
EMU=emulator-5554
EMU_5556=emulator-5556
V880=Blade-CM7-Harley
G2D=0A3AAA1E0200E01F


all:

_DEVICE=${${DEVICE}}

# APK=$(shell echo ${ACTIVITY} | tr A-Z a-z)-debug.apk
APK=${PROJECT}-debug.apk
ADB=adb -s ${_DEVICE}
TAGS=${PROJECT}:* *:e

bin/${APK}:	force
	ant debug

install:	bin/${APK}
	${ADB} install -r $<

run:	
	${ADB} logcat -c # clear the logs
	${ADB} shell am start -n ${PACKAGE}/.${ACTIVITY}

log:	
	${ADB} logcat -s ${TAGS}

all:
	make install
	make run
	make log

force:
