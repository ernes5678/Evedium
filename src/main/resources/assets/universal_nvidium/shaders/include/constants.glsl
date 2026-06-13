#version 430 core

const float PI = 3.14159265359;
const float DEG2RAD = PI / 180.0;
const int MAX_SECTIONS = 4096;
const int WORK_GROUP_SIZE = 64;
