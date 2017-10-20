#ifndef ANDROID_IMAGE_CLASSIFICATION_IMAGE_RESIZE_H
#define ANDROID_IMAGE_CLASSIFICATION_IMAGE_RESIZE_H

#include <stdint.h>
#include <cmath>

void resize_image(uint8_t* const pixelsSrc, const int old_cols, const int old_rows, uint8_t* const pixelsTarget, int const new_cols, int const new_rows);

#endif //ANDROID_IMAGE_CLASSIFICATION_IMAGE_RESIZE_H
