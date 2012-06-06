/**
 *   ForceFeedback Joystick Driver for Java
 *   http://sourceforge.net/projects/ffjoystick4java/
 *   
 *   -----------------------------------------------------------------------------
 *   
 *   Copyright (c) 2010, Martin Wischenbart
 *   All rights reserved.
 *   
 *   Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of the ForceFeedback Joystick Driver for Java nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 *      
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *   ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *   LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *   ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE.
 *   
 *   -----------------------------------------------------------------------------
 *   
 *   If you have any suggestions, or if you want to report a bug, please
 *   see http://sourceforge.net/projects/ffjoystick4java/ or contact me
 *   via email.
 *   
 *   Martin Wischenbart
 *   elboato@users.sourceforge.net
 *   
 */



// PRECOMPILER SETTING:
// On windows we have to cope with the odd effect of the axis order being
// reversed on some controllers.
// - Set INVERTAXISORDER to 1 if you compile for Windows and want to invert
//   the order of the analogue axes.
// - Set INVERTAXISORDER to 0 otherwise.
#define INVERTAXISORDER 0


//-----------------------------------------------------------------------------
// Includes
//-----------------------------------------------------------------------------

#include <stdio.h>
#include <stdlib.h>

#include <jni.h>
#include "FFJoystick.h"

#include <SDL.h>
#include <SDL_haptic.h>

//-----------------------------------------------------------------------------
// Storage of Joysticks and FFJoysticks by index
//-----------------------------------------------------------------------------

// The system will allocate memory for NUMOFJOYSTICKS pointers.
#define NUMOFJOYSTICKS 64

static SDL_Joystick *joysticks[NUMOFJOYSTICKS];
static SDL_Haptic *ffjoysticks[NUMOFJOYSTICKS];

//-----------------------------------------------------------------------------
// throwing SDLErrorException
//-----------------------------------------------------------------------------

// To make throwing Exceptions work it is necessary to call the Java program with
// the java.class.path-variable set. (parameter -Djava.class.path=.)

int throwException(JNIEnv *env, const char *errorMessage) {
	jclass exceptionClass = env->FindClass("at/wisch/joystick/exception/SDLErrorException");
	if (!exceptionClass) {
		printf("Fatal Error! Could not throw SDLErrorException.\nDid you set the java.class.path-variable correctly?\n");
		exit(-99);
		return -99;
	}
	env->ThrowNew(exceptionClass, errorMessage);
	return 0;
}

//-----------------------------------------------------------------------------
// Functions for class at_wisch_joystick_JoystickManager
//-----------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_at_wisch_joystick_JoystickManager_sdlInitNative(JNIEnv *env, jclass)
{
	if(SDL_Init(SDL_INIT_JOYSTICK | SDL_INIT_HAPTIC))
	{
		throwException(env, SDL_GetError());
		return -1;
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_JoystickManager_getNumOfJoysticks (JNIEnv *, jclass) {
	return SDL_NumJoysticks();
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_JoystickManager_openJoystick (JNIEnv *env, jclass, jint joystickIndex) {
	joysticks[joystickIndex] = SDL_JoystickOpen(joystickIndex);
	if (!SDL_JoystickOpened(joystickIndex)){
		throwException(env, SDL_GetError());
		return -2;
	}
	return 2;
}

	
JNIEXPORT jint JNICALL Java_at_wisch_joystick_JoystickManager_openFFJoystick (JNIEnv *env, jclass, jint joystickIndex) {
	SDL_Haptic* hapticDevice = SDL_HapticOpenFromJoystick(joysticks[joystickIndex]);
	if (hapticDevice == NULL) {
		// Most likely Joystick does not have FF-capabilities...
		return -4;
	}
	
	int hapticDeviceIndex = SDL_HapticIndex(hapticDevice);
	/* # the previous line did not work with previous versions of SDL 
	   # see http://bugzilla.libsdl.org/show_bug.cgi?id=946
	   # -> had to use the following workaround
	   # (the implementation of SDL_HapticOpenFromJoystick in linux in fact does it it in the same way) */
	/* int hapticDeviceIndex = -5;
	for (int i = 0; i < SDL_NumHaptics(); i++) {
		if (SDL_HapticName(i) != NULL) {
			if (SDL_strcmp(SDL_HapticName(i), SDL_JoystickName(joystickIndex)) == 0) {
		        hapticDeviceIndex = i;
			    break;
			}
        }
    }*/

	if (hapticDeviceIndex < 0) {	
		throwException(env, SDL_GetError());
		return hapticDeviceIndex;
	}	
	ffjoysticks[hapticDeviceIndex] = hapticDevice;
	return hapticDeviceIndex;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_JoystickManager_sdlInitEventsNative (JNIEnv *env, jclass)
{
	// Event handling is initialised (along with video)
	if(SDL_Init(SDL_INIT_VIDEO))
	{
		throwException(env, SDL_GetError());
		return -11;
	}
	int state = SDL_JoystickEventState(SDL_IGNORE);
	if (state != SDL_IGNORE) {
		throwException(env, SDL_GetError());
		return -13;
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_JoystickManager_enableJoystickEventPollingNative (JNIEnv *env, jclass){
	int state = SDL_JoystickEventState(SDL_ENABLE);
	if (state != SDL_ENABLE) {
		throwException(env, SDL_GetError());
		return -12;
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_JoystickManager_disableJoystickEventPollingNative(JNIEnv *env, jclass){
	int state = SDL_JoystickEventState(SDL_IGNORE);
	if (state != SDL_IGNORE) {
		throwException(env, SDL_GetError());
		return -13;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_at_wisch_joystick_JoystickManager_closeSDLnative (JNIEnv *, jclass) {
	SDL_Quit();
}

//-----------------------------------------------------------------------------
// Functions for class at_wisch_joystick_EventPollThread
//-----------------------------------------------------------------------------

// helper function
int translateHatState (int hatState) {
	if (hatState == SDL_HAT_CENTERED) return at_wisch_joystick_Joystick_POV_CENTERED;
	
	if (hatState == SDL_HAT_RIGHTUP) return at_wisch_joystick_Joystick_POV_UP_RIGHT;
	if (hatState == SDL_HAT_RIGHTDOWN) return at_wisch_joystick_Joystick_POV_DOWN_RIGHT;
	if (hatState == SDL_HAT_LEFTDOWN) return at_wisch_joystick_Joystick_POV_DOWN_LEFT;
	if (hatState == SDL_HAT_LEFTUP) return at_wisch_joystick_Joystick_POV_UP_LEFT;

	if (hatState == SDL_HAT_UP) return at_wisch_joystick_Joystick_POV_UP;
	if (hatState == SDL_HAT_RIGHT) return at_wisch_joystick_Joystick_POV_RIGHT;
	if (hatState == SDL_HAT_DOWN) return at_wisch_joystick_Joystick_POV_DOWN;
	if (hatState == SDL_HAT_LEFT) return at_wisch_joystick_Joystick_POV_LEFT;

	return -99;
}

JNIEXPORT jintArray JNICALL Java_at_wisch_joystick_EventPollThread_pollForEvent (JNIEnv *env, jobject){
	
	int returnArray[7] = { -8, -8, 0, 0, 0, 0};
	/* index 0: eventStored
	 * index 1: joystickIndex
	 * index 2: eventType
	 * index 3: componentIndex
	 * index 4: value1
	 * index 5: value2
	 */
	
	SDL_Event event;

	/*
	 * According to the SDL documentation it should not be necessary, but i did not get
	 * any events without calling JoystickUpdate manually here.
	 */
	SDL_JoystickUpdate();

	returnArray[0] = SDL_PeepEvents(&event, 1, SDL_GETEVENT, SDL_JOYEVENTMASK);
	
	if (returnArray[0]){
		if (!event.type) {
			throwException(env, SDL_GetError());
			returnArray[0] = -14;
		} else {
			switch (event.type) {
				case (SDL_JOYBUTTONDOWN): //see SDL_JOYBUTTONUP below
				case (SDL_JOYBUTTONUP) : {
					returnArray[1] = event.jbutton.which;
					returnArray[2] = 1;
					returnArray[3] = event.jbutton.button;
					returnArray[4] = event.jbutton.state;
					returnArray[5] = 0;
					break; }
				case SDL_JOYHATMOTION: {
					returnArray[1] = event.jhat.which;
					returnArray[2] = 5;
					returnArray[3] = event.jhat.hat;
					returnArray[4] = translateHatState(event.jhat.value);
					returnArray[5] = 0;
					break; }
				case SDL_JOYAXISMOTION: {
					returnArray[1] = event.jaxis.which;
					returnArray[2] = 2;
					returnArray[3] = event.jaxis.axis;
					if (INVERTAXISORDER) returnArray[3] = SDL_JoystickNumAxes(joysticks[returnArray[1]])-returnArray[3]-1;
					returnArray[4] = event.jaxis.value;
					returnArray[5] = 0;
					break; }
				case SDL_JOYBALLMOTION: {
					returnArray[1] = event.jball.which;
					returnArray[2] = 6;
					returnArray[3] = event.jball.ball;
					returnArray[4] = event.jball.xrel;
					returnArray[5] = event.jball.yrel;
					break; }
			}
		}
	}
	jintArray returnJArray = (jintArray)env->NewIntArray(6);
	env->SetIntArrayRegion(returnJArray, 0, 6, (jint *)returnArray);
	return (returnJArray);
}

//-----------------------------------------------------------------------------
// Functions for class at_wisch_joystick_Joystick
//-----------------------------------------------------------------------------

JNIEXPORT jstring JNICALL Java_at_wisch_joystick_Joystick_getJoystickName (JNIEnv *env, jclass, jint joystickIndex) {
	const char *name = SDL_JoystickName(joystickIndex);
	if (name == NULL) {
		throwException(env, SDL_GetError());
		return env->NewStringUTF("-6");
	}
	return env->NewStringUTF(name);
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getNoOfButtons (JNIEnv *env, jclass, jint joystickIndex){
	int num = SDL_JoystickNumButtons(joysticks[joystickIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -7;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getNoOfPovs (JNIEnv *env, jclass, jint joystickIndex){
	int num =  SDL_JoystickNumHats(joysticks[joystickIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -8;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getNoOfAxes (JNIEnv *env, jclass, jint joystickIndex){
	int num = SDL_JoystickNumAxes(joysticks[joystickIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -9;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getNoOfBalls (JNIEnv *env, jclass, jint joystickIndex){
	int num = SDL_JoystickNumBalls(joysticks[joystickIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -10;
	}
	return num;
}

JNIEXPORT void JNICALL Java_at_wisch_joystick_Joystick_poll (JNIEnv *, jobject) {
	SDL_JoystickUpdate();
}

JNIEXPORT jboolean JNICALL Java_at_wisch_joystick_Joystick_isButtonPressedNative (JNIEnv *, jclass, jint joystickIndex, jint buttonIndex) {
	return SDL_JoystickGetButton(joysticks[joystickIndex], buttonIndex);
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getPovValueXNative (JNIEnv *, jobject, jint joystickIndex, jint hatIndex){
	int value = SDL_JoystickGetHat(joysticks[joystickIndex], hatIndex);
	if (value & SDL_HAT_RIGHT) {
		return at_wisch_joystick_Joystick_POV_AXIS_POSITIVE;
	} else if (value & SDL_HAT_LEFT) {
		return at_wisch_joystick_Joystick_POV_AXIS_NEGATIVE;
	} else {
		return at_wisch_joystick_Joystick_POV_AXIS_NEUTRAL;
	}
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getPovValueYNative (JNIEnv *, jobject, jint joystickIndex, jint hatIndex){
	int value = SDL_JoystickGetHat(joysticks[joystickIndex], hatIndex);
	if (value & SDL_HAT_DOWN) {
		return at_wisch_joystick_Joystick_POV_AXIS_POSITIVE;
	} else if (value & SDL_HAT_UP) {
		return at_wisch_joystick_Joystick_POV_AXIS_NEGATIVE;
	} else {
		return at_wisch_joystick_Joystick_POV_AXIS_NEUTRAL;
	}
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getPovDirectionNative (JNIEnv *, jobject, jint joystickIndex, jint hatIndex){
	return translateHatState(SDL_JoystickGetHat(joysticks[joystickIndex], hatIndex));
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_Joystick_getAxisValueNative (JNIEnv *, jclass, jint joystickIndex, jint axisIndex) {
	if (INVERTAXISORDER) axisIndex = SDL_JoystickNumAxes(joysticks[joystickIndex])-axisIndex-1;
	return SDL_JoystickGetAxis(joysticks[joystickIndex], axisIndex);
}

JNIEXPORT jintArray JNICALL Java_at_wisch_joystick_Joystick_getBallDeltaNative (JNIEnv *env, jobject, jint joystickIndex, jint ballIndex) {
	// TODO: This is untested, because I don't have a joystick with trackball.
	int deltaArray[2];
	if (!SDL_JoystickGetBall(joysticks[joystickIndex], ballIndex, &deltaArray[0], &deltaArray[1])) {
		//throwException(env, SDL_GetError()); + also add throws at declaration of Java method
		deltaArray[0] = 0;
		deltaArray[1] = 0;
	}
	jintArray jDeltaArray = (jintArray)env->NewIntArray(2);
	env->SetIntArrayRegion(jDeltaArray, 0, 2, (jint *)deltaArray);
	return (jDeltaArray);
}

//-----------------------------------------------------------------------------
// Functions for class at_wisch_joystick_ff_FFJoystick
//-----------------------------------------------------------------------------

JNIEXPORT jbooleanArray JNICALL Java_at_wisch_joystick_FFJoystick_getFFCapabilitiesNative (JNIEnv *env, jclass, jint hapticDeviceIndex) {
	int supported;
	bool capabilitiesArray[16];
	SDL_HapticEffect effect;
 
	// EFFECTS
	effect.type = SDL_HAPTIC_CONSTANT;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[0] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[0] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[0] = false;
	}
	
	effect.type = SDL_HAPTIC_SINE;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[1] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[1] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[1] = false;
	}

	effect.type = SDL_HAPTIC_SQUARE;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[2] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[2] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[2] = false;
	}

	effect.type = SDL_HAPTIC_TRIANGLE;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[3] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[3] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[3] = false;
	}

	effect.type = SDL_HAPTIC_SAWTOOTHUP;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[4] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[4] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[4] = false;
	}

	effect.type = SDL_HAPTIC_SAWTOOTHDOWN;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[5] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[5] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[5] = false;
	}

	effect.type = SDL_HAPTIC_RAMP;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[6] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[6] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[6] = false;
	}

	effect.type = SDL_HAPTIC_SPRING;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[7] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[7] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[7] = false;
	}

	effect.type = SDL_HAPTIC_DAMPER;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[8] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[8] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[8] = false;
	}

	effect.type = SDL_HAPTIC_INERTIA;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[9] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[9] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[9] = false;
	}

	effect.type = SDL_HAPTIC_FRICTION;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[10] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[10] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[10] = false;
	}

	effect.type = SDL_HAPTIC_CUSTOM;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[11] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[11] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[11] = false;
	}

	// OTHER
	// THIS IS ACTUALLY LOGICALLY WRONG (effect.type should be neither of the
	//		following 4) - because of information hiding (we cannot access the field
	//		unsigned int supported (Supported effects) in SDL_Haptic from here)
	//		we have to do it this way....

	effect.type = SDL_HAPTIC_GAIN;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[12] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[12] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[12] = false;
	}

	effect.type = SDL_HAPTIC_AUTOCENTER;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[13] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[13] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[13] = false;
	}

	effect.type = SDL_HAPTIC_STATUS;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[14] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[14] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[14] = false;
	}

	effect.type = SDL_HAPTIC_PAUSE;
	supported = SDL_HapticEffectSupported(ffjoysticks[hapticDeviceIndex], &effect);
	if (supported == SDL_TRUE) {
		capabilitiesArray[15] = true;
	} else if (supported == SDL_FALSE) {
		capabilitiesArray[15] = false;
	} else {
		throwException(env, SDL_GetError());
		capabilitiesArray[15] = false;
	}
	jbooleanArray jCapabilitiesArray = (jbooleanArray)env->NewBooleanArray(16);
	env->SetBooleanArrayRegion(jCapabilitiesArray, 0, 16, (jboolean *)capabilitiesArray);
	return (jCapabilitiesArray);

}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_getNumOfAxesNative (JNIEnv *env, jclass, jint hapticDeviceIndex) {
	int num = SDL_HapticNumAxes(ffjoysticks[hapticDeviceIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -15;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_getPlayNumOfEffectsNative (JNIEnv *env, jclass, jint hapticDeviceIndex){
	int num = SDL_HapticNumEffectsPlaying(ffjoysticks[hapticDeviceIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -16;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_getStoreNumOfEffectsNative (JNIEnv *env, jclass, jint hapticDeviceIndex){
	int num = SDL_HapticNumEffects(ffjoysticks[hapticDeviceIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -17;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_setAutoCenterNative (JNIEnv *env, jclass, jint hapticDeviceIndex, jint autocenterValue){
	int num = SDL_HapticSetAutocenter(ffjoysticks[hapticDeviceIndex], autocenterValue);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -17;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_setGainNative (JNIEnv *env, jclass, jint hapticDeviceIndex, jint gainValue){
	int num = SDL_HapticSetGain(ffjoysticks[hapticDeviceIndex], gainValue);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -18;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_pause (JNIEnv *env, jclass, jint hapticDeviceIndex){
	int num = SDL_HapticPause(ffjoysticks[hapticDeviceIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -19;
	}
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_unpause (JNIEnv *env, jclass, jint hapticDeviceIndex){
	int num = SDL_HapticUnpause(ffjoysticks[hapticDeviceIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -20;
	}
	return num;
}

SDL_HapticEffect* getEffectFromJavaEffect (JNIEnv *env, jobject jEffect, jint effectType) {

	SDL_HapticEffect *effect = new SDL_HapticEffect;

	jclass clazz=env->GetObjectClass(jEffect);
	
	// COMMON FOR ALL EFFECTS:
	
	int effectLength = env->GetIntField(jEffect, env->GetFieldID(clazz, "effectLength", "I"));
	if ( effectLength == at_wisch_joystick_ffeffect_Effect_INFINITE_LENGTH ) {
		effectLength = SDL_HAPTIC_INFINITY;
	}
	int effectDelay = env->GetIntField(jEffect, env->GetFieldID(clazz, "effectDelay", "I"));
	int buttonIndex = (env->GetIntField(jEffect, env->GetFieldID(clazz, "buttonIndex", "I")))+1; // ATTENTION: In SDL_haptic Buttons start at index 1 instead of index 0 like they joystick.
	if (buttonIndex == at_wisch_joystick_ffeffect_Effect_NO_BUTTON) buttonIndex = 0;
	int buttonInterval = env->GetIntField(jEffect, env->GetFieldID(clazz, "buttonInterval", "I"));
	
	// COMMON FOR ALL EFFECTS EXCEPT CONDITION EFFECTS:

	int attackLength = 0;
	int fadeLength = 0;
	int attackLevel = 0;
	int fadeLevel = 0;
	int directionType = 0;
	int directionValues[3] = {0,0,0};

	if (effectType != at_wisch_joystick_ffeffect_Effect_EFFECT_SPRING &&
		effectType != at_wisch_joystick_ffeffect_Effect_EFFECT_DAMPER &&
		effectType != at_wisch_joystick_ffeffect_Effect_EFFECT_INERTIA &&
		effectType != at_wisch_joystick_ffeffect_Effect_EFFECT_FRICTION)
		{
			attackLength = env->GetIntField(jEffect, env->GetFieldID(clazz, "attackLength", "I"));
			fadeLength = env->GetIntField(jEffect, env->GetFieldID(clazz, "fadeLength", "I"));
			attackLevel = env->GetIntField(jEffect, env->GetFieldID(clazz, "attackLevel", "I"));
			fadeLevel = env->GetIntField(jEffect, env->GetFieldID(clazz, "fadeLevel", "I"));

			jfieldID jDirectionField = env->GetFieldID(clazz, "direction", "Lat/wisch/joystick/ffeffect/direction/Direction;");
			jobject jDirection = env->GetObjectField(jEffect, jDirectionField );
			jclass directionClazz=env->GetObjectClass(jDirection);
			switch(env->GetIntField(jDirection, env->GetFieldID(directionClazz, "directionType", "I"))) {
				case (at_wisch_joystick_ffeffect_direction_Direction_DIRECTION_POLAR): {
					directionType = SDL_HAPTIC_POLAR;
					directionValues[0] = env->GetIntField(jDirection, env->GetFieldID(directionClazz, "polarDirection", "I"));
					directionValues[1] = 0;
					directionValues[2] = 0;
					break; }
				case (at_wisch_joystick_ffeffect_direction_Direction_DIRECTION_SPHERICAL): {
					directionType = SDL_HAPTIC_SPHERICAL;
					jfieldID jArrayID = env->GetFieldID(directionClazz, "sphericalCoordinates", "[I");
					jintArray jArray = (jintArray)(env->GetObjectField(jDirection, jArrayID));
					jint *array = env->GetIntArrayElements(jArray, NULL);
					directionValues[0] = array[0];
					directionValues[1] = array[1];
					directionValues[2] = 0;
					break; }
				case (at_wisch_joystick_ffeffect_direction_Direction_DIRECTION_CARTESIAN): {
					directionType = SDL_HAPTIC_CARTESIAN;
					jfieldID jArrayID = env->GetFieldID(directionClazz, "cartesianCoordinates", "[I");
					jintArray jArray = (jintArray)(env->GetObjectField(jDirection, jArrayID));
					jint *array = env->GetIntArrayElements(jArray, NULL);
					directionValues[0] = array[0];
					directionValues[1] = array[1];
					directionValues[2] = array[2];
					if (INVERTAXISORDER) {
						/* TODO: stay compatible/test for joysticks with 1 or 3 ff-axes */
						directionValues[0] = array[1];
						directionValues[1] = array[0];
						//directionValues[2] = array[2];
					}
					break; }
			}
	}

	switch(effectType){

		// CONSTANT EFFECT:

		case (at_wisch_joystick_ffeffect_Effect_EFFECT_CONSTANT): {
			effect->type = SDL_HAPTIC_CONSTANT;
			effect->constant.type = SDL_HAPTIC_CONSTANT;
			effect->constant.level = env->GetIntField(jEffect, env->GetFieldID(clazz, "level", "I"));
			
			effect->constant.direction.type = directionType;
			effect->constant.direction.dir[0] = directionValues[0];
			effect->constant.direction.dir[1] = directionValues[1];
			effect->constant.direction.dir[2] = directionValues[2];

			effect->constant.length = effectLength;
			effect->constant.delay = effectDelay;
			effect->constant.button = buttonIndex;
			effect->constant.interval = buttonInterval;
			effect->constant.attack_length = attackLength;
			effect->constant.fade_length = fadeLength;
			effect->constant.attack_level = attackLevel;
			effect->constant.fade_level = fadeLevel;
			break; }


		// PERIODIC EFFECT:

		case (at_wisch_joystick_ffeffect_Effect_EFFECT_SINE) :
		case (at_wisch_joystick_ffeffect_Effect_EFFECT_SQUARE) :
		case (at_wisch_joystick_ffeffect_Effect_EFFECT_TRIANGLE) :
		case (at_wisch_joystick_ffeffect_Effect_EFFECT_SAWTOOTHUP) :
		case (at_wisch_joystick_ffeffect_Effect_EFFECT_SAWTOOHDOWN) : {

			effect->periodic.period = env->GetIntField(jEffect, env->GetFieldID(clazz, "period", "I"));
			effect->periodic.magnitude = env->GetIntField(jEffect, env->GetFieldID(clazz, "magnitude", "I"));
			effect->periodic.offset = env->GetIntField(jEffect, env->GetFieldID(clazz, "offset", "I"));
			effect->periodic.phase = env->GetIntField(jEffect, env->GetFieldID(clazz, "phase", "I"));
			
			if (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_SINE) {
				effect->type = SDL_HAPTIC_SINE;
				effect->periodic.type = SDL_HAPTIC_SINE;
			} else if (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_SQUARE) {
				effect->type = SDL_HAPTIC_SQUARE;
				effect->periodic.type = SDL_HAPTIC_SQUARE;
			} else if (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_TRIANGLE) {
				effect->type = SDL_HAPTIC_TRIANGLE;
				effect->periodic.type = SDL_HAPTIC_TRIANGLE;
			} else if (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_SAWTOOTHUP) {
				effect->type = SDL_HAPTIC_SAWTOOTHUP;
				effect->periodic.type = SDL_HAPTIC_SAWTOOTHUP;
			} else { // (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_SAWTOOTHDOWN)
				effect->type = SDL_HAPTIC_SAWTOOTHDOWN;
				effect->periodic.type = SDL_HAPTIC_SAWTOOTHDOWN; 
			}

			effect->periodic.direction.type = directionType;
			effect->periodic.direction.dir[0] = directionValues[0];
			effect->periodic.direction.dir[1] = directionValues[1];
			effect->periodic.direction.dir[2] = directionValues[2];
			effect->periodic.length = effectLength;
			effect->periodic.delay = effectDelay;
			effect->periodic.button = buttonIndex;
			effect->periodic.interval = buttonInterval;
			effect->periodic.attack_length = attackLength;
			effect->periodic.fade_length = fadeLength;
			effect->periodic.attack_level = attackLevel;
			effect->periodic.fade_level = fadeLevel;
			break; }


		// RAMP EFFECT:

		case (at_wisch_joystick_ffeffect_Effect_EFFECT_RAMP): {
			effect->type = SDL_HAPTIC_RAMP;
			effect->ramp.type = SDL_HAPTIC_RAMP;
			effect->ramp.start = env->GetIntField(jEffect, env->GetFieldID(clazz, "startLevel", "I"));
			effect->ramp.end = env->GetIntField(jEffect, env->GetFieldID(clazz, "endLevel", "I"));

			effect->ramp.direction.type = directionType;
			effect->ramp.direction.dir[0] = directionValues[0];
			effect->ramp.direction.dir[1] = directionValues[1];
			effect->ramp.direction.dir[2] = directionValues[2];
			effect->ramp.length = effectLength;
			effect->ramp.delay = effectDelay;
			effect->ramp.button = buttonIndex;
			effect->ramp.interval = buttonInterval;
			effect->ramp.attack_length = attackLength;
			effect->ramp.fade_length = fadeLength;
			effect->ramp.attack_level = attackLevel;
			effect->ramp.fade_level = fadeLevel;
			break; }

													   
		// CONDITION EFFECT:

		case (at_wisch_joystick_ffeffect_Effect_EFFECT_SPRING) :
		case (at_wisch_joystick_ffeffect_Effect_EFFECT_DAMPER) :
		case (at_wisch_joystick_ffeffect_Effect_EFFECT_INERTIA) :
		case (at_wisch_joystick_ffeffect_Effect_EFFECT_FRICTION) : {
			
			
			jfieldID jArrayID = env->GetFieldID(clazz, "rightSat", "[I");
			jintArray jArray = (jintArray)(env->GetObjectField(jEffect, jArrayID));
			jint *array = env->GetIntArrayElements(jArray, NULL);
			int i;
			for (i = 0; i<3; i++) {
				effect->condition.right_sat[i] = array[i];
			}
			jArrayID = env->GetFieldID(clazz, "leftSat", "[I");
			jArray = (jintArray)(env->GetObjectField(jEffect, jArrayID));
			array = env->GetIntArrayElements(jArray, NULL);
			for (i = 0; i<3; i++) {
				effect->condition.left_sat[i] = array[i];
			}
			jArrayID = env->GetFieldID(clazz, "rightCoef", "[I");
			jArray = (jintArray)(env->GetObjectField(jEffect, jArrayID));
			array = env->GetIntArrayElements(jArray, NULL);
			for (i = 0; i<3; i++) {
				effect->condition.right_coeff[i] = array[i];
			}
			jArrayID = env->GetFieldID(clazz, "leftCoef", "[I");
			jArray = (jintArray)(env->GetObjectField(jEffect, jArrayID));
			array = env->GetIntArrayElements(jArray, NULL);
			for (i = 0; i<3; i++) {
				effect->condition.left_coeff[i] = array[i];
			}
			jArrayID = env->GetFieldID(clazz, "deadbandInt", "[I");
			jArray = (jintArray)(env->GetObjectField(jEffect, jArrayID));
			array = env->GetIntArrayElements(jArray, NULL);
			for (i = 0; i<3; i++) {
				effect->condition.deadband[i] = array[i];
			} 
			jArrayID = env->GetFieldID(clazz, "centerInt", "[I");
			jArray = (jintArray)(env->GetObjectField(jEffect, jArrayID));
			array = env->GetIntArrayElements(jArray, NULL);
			for (i = 0; i<3; i++) {
				effect->condition.center[i] = array[i];
			}
			

			if (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_SPRING) {
				effect->type = SDL_HAPTIC_SPRING;
				effect->condition.type = SDL_HAPTIC_SPRING;
			} else if (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_DAMPER) {
				effect->type = SDL_HAPTIC_DAMPER;
				effect->condition.type = SDL_HAPTIC_DAMPER;
			} else if (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_INERTIA) {
				effect->type = SDL_HAPTIC_INERTIA;
				effect->condition.type = SDL_HAPTIC_INERTIA;
			} else { // (effectType == at_wisch_joystick_ffeffect_Effect_EFFECT_FRICTION) {
				effect->type = SDL_HAPTIC_FRICTION;
				effect->condition.type = SDL_HAPTIC_FRICTION;
			}
			
			effect->condition.length = effectLength;
			effect->condition.delay = effectDelay;
			effect->condition.button = buttonIndex;
			effect->condition.interval = buttonInterval;

			// set some dummy direction 
			effect->condition.direction.type = SDL_HAPTIC_POLAR;
			effect->condition.direction.dir[0] = 0; //NORTH
			effect->condition.direction.dir[1] = 0;
			effect->condition.direction.dir[2] = 0;
			break; }

		// CUSTOM EFFECT:

		case (at_wisch_joystick_ffeffect_Effect_EFFECT_CUSTOM): {
			effect->type = SDL_HAPTIC_CUSTOM;
			effect->custom.type = SDL_HAPTIC_CUSTOM;
			
			effect->custom.period = env->GetIntField(jEffect, env->GetFieldID(clazz, "period", "I"));
			effect->custom.channels = env->GetIntField(jEffect, env->GetFieldID(clazz, "channelsToUse", "I"));
			effect->custom.samples = env->GetIntField(jEffect, env->GetFieldID(clazz, "numOfSamples", "I"));

			jfieldID jArrayID = env->GetFieldID(clazz, "data", "[I");
			jintArray jArray = (jintArray)(env->GetObjectField(jEffect, jArrayID));
			jint *jDataArray = env->GetIntArrayElements(jArray, NULL);
			
			effect->custom.data = new Uint16[effect->custom.samples * effect->custom.channels];
			for (int i = 0; i < (effect->custom.samples * effect->custom.channels); i++) {
				effect->custom.data[i] = jDataArray[i];
				// printf("C %i ", effect->custom.data[i]);
			}
			
			effect->custom.direction.type = directionType;
			effect->custom.direction.dir[0] = directionValues[0];
			effect->custom.direction.dir[1] = directionValues[1];
			effect->custom.direction.dir[2] = directionValues[2];
			effect->custom.length = effectLength;
			effect->custom.delay = effectDelay;
			effect->custom.button = buttonIndex;
			effect->custom.interval = buttonInterval;
			effect->custom.attack_length = attackLength;
			effect->custom.fade_length = fadeLength;
			effect->custom.attack_level = attackLevel;
			effect->custom.fade_level = fadeLevel;

			//printf(" %i %i %i %i ", effect->custom.length, effect->custom.period, effect->custom.channels, effect->custom.samples);

			break; }
	}
	return effect;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_newEffectNative (JNIEnv *env, jclass, jint hapticDeviceIndex, jobject jEffect, jint effectType){
	SDL_HapticEffect *effect = getEffectFromJavaEffect(env, jEffect, effectType);
	int num = SDL_HapticNewEffect(ffjoysticks[hapticDeviceIndex], effect);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -21;
	}
	
	if (effect->type == SDL_HAPTIC_CUSTOM){ delete(effect->custom.data); }
	delete(effect);
	return num;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_playEffectNative(JNIEnv *env, jclass, jint hapticDeviceIndex, jint effectIndex, jint iterations){
	if (iterations == at_wisch_joystick_Joystick_INFINITE_TIMES){
		iterations = SDL_HAPTIC_INFINITY;
	}
	int num = SDL_HapticRunEffect(ffjoysticks[hapticDeviceIndex], effectIndex, iterations);
	if (num < 0) {
		throwException(env, SDL_GetError()); 
		return -22;
	}
 	return num;
}

JNIEXPORT jboolean JNICALL Java_at_wisch_joystick_FFJoystick_getEffectStatusNative (JNIEnv *env, jclass, jint hapticDeviceIndex, jint effectIndex){
	int num = SDL_HapticGetEffectStatus(ffjoysticks[hapticDeviceIndex], effectIndex);
	if (num > 0) {
		return true;
	} else if (num == 0) {
		return false;
	} else {
		throwException(env, SDL_GetError());
		return false;
	}
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_updateEffectNative (JNIEnv *env, jclass, jint hapticDeviceIndex, jint effectIndex, jobject jEffect, jint effectType) {
	SDL_HapticEffect *effect = getEffectFromJavaEffect(env, jEffect, effectType);
	int num = SDL_HapticUpdateEffect(ffjoysticks[hapticDeviceIndex], effectIndex, effect);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -24;
	}
	if (effect->type == SDL_HAPTIC_CUSTOM){ delete(effect->custom.data); }
	delete(effect);

	return 0;
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_stopEffectNative (JNIEnv *env, jclass, jint hapticDeviceIndex, jint effectIndex){
	int num = SDL_HapticStopEffect(ffjoysticks[hapticDeviceIndex], effectIndex);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -25;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_at_wisch_joystick_FFJoystick_destroyEffectNative (JNIEnv *env, jclass, jint hapticDeviceIndex, jint effectIndex) {
	SDL_HapticDestroyEffect(ffjoysticks[hapticDeviceIndex], effectIndex);
}

JNIEXPORT jint JNICALL Java_at_wisch_joystick_FFJoystick_stopAllNative (JNIEnv *env, jclass, jint hapticDeviceIndex){
	int num = SDL_HapticStopAll(ffjoysticks[hapticDeviceIndex]);
	if (num < 0) {
		throwException(env, SDL_GetError());
		return -27;
	}
	return 0;
}

JNIEXPORT void JNICALL Java_at_wisch_joystick_FFJoystick_destroyAllNative (JNIEnv *env, jclass, jint hapticDeviceIndex) {
	for (int i = SDL_HapticNumEffects(ffjoysticks[hapticDeviceIndex]); i >= 0; i--) {
		SDL_HapticDestroyEffect(ffjoysticks[hapticDeviceIndex], i);
	}
}