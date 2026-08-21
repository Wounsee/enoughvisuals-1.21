cmake_minimum_required(VERSION 3.20)
project(enough_cef_audio LANGUAGES CXX)

if(NOT DEFINED CEF_ROOT)
    message(FATAL_ERROR "Pass -DCEF_ROOT=<cef_binary_116..._windows64>")
endif()

add_library(enough_cef_audio SHARED cef_audio_bridge.cpp)
target_compile_features(enough_cef_audio PRIVATE cxx_std_17)
target_include_directories(enough_cef_audio PRIVATE "${CEF_ROOT}")
target_compile_definitions(enough_cef_audio PRIVATE USING_CEF_SHARED)
set_target_properties(enough_cef_audio PROPERTIES PREFIX "")
if(MSVC)
    set_property(TARGET enough_cef_audio PROPERTY
            MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>")
endif()
