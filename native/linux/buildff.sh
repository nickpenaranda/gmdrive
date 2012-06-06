sudo g++ `sdl-config --cflags --libs` -I/usr/lib/jvm/java-6-sun/include/ -I/usr/lib/jvm/java-6-sun/include/linux/ -I/usr/local/lib/ -o /usr/local/lib/libffjoystick.so -shared FFJoystick.cpp
