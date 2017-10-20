#include "image_resize.h"

double lanczos_size_ = 3.0;
int num_channels = 4;


inline double sinc(double x) {
    double pi = 3.1415926;
    x = (x * pi);
    if (x < 0.01 && x > -0.01)
        return 1.0 + x*x*(-1.0/6.0 + x*x*1.0/120.0);
    return sin(x)/x;
}

inline double LanczosFilter(double x) {
    if (std::abs(x) < lanczos_size_) {
        return sinc(x)*sinc(x/lanczos_size_);
    } else {
        return 0.0;
    }
}

void resize_image(uint8_t* const pixelsSrc, const int old_cols, const int old_rows, uint8_t* const pixelsTarget, int const new_cols, int const new_rows ) {

    double col_ratio =
            static_cast<double>(old_cols) / static_cast<double>(new_cols);
    double row_ratio =
            static_cast<double>(old_rows) / static_cast<double>(new_rows);

    // Now apply a filter to the image.
    for (int row = 0; row < new_rows; ++row) {
        const double row_within = static_cast<double>(row) * row_ratio;
        int floor_row = static_cast<int>(row_within);
        for (int col = 0; col < new_cols; ++col) {
            for (int channel = 0; channel < num_channels; ++channel) {
                // x is the new col in terms of the old col coordinates.
                double col_within = static_cast<double>(col) * col_ratio;
                // The old col corresponding to the closest new col.
                int floor_col = static_cast<int>(col_within);

                uint8_t &v_toSet = pixelsTarget[row * new_cols * num_channels + col * num_channels + channel];
                v_toSet = 0;
                double weight = 0.0;
                for (int i = floor_row - lanczos_size_ + 1; i <= floor_row + lanczos_size_; ++i) {
                    for (int j = floor_col - lanczos_size_ + 1;
                         j <= floor_col + lanczos_size_; ++j) {
                        if (i >= 0 && i < old_rows && j >= 0 && j < old_cols) {
                            const double lanc_term = LanczosFilter(row_within - i + col_within - j);
                            v_toSet += static_cast<uint8_t>(pixelsSrc[i * old_cols * num_channels + j * num_channels + channel] *
                                                            lanc_term);
                            weight += lanc_term;
                        }
                    }
                }

                v_toSet /= static_cast<uint8_t>(weight);
                v_toSet = (v_toSet > 255) ? 255 : v_toSet;
                v_toSet = (v_toSet < 0) ? 0 : v_toSet;
            }
        }
    }
}
