/*
 *   tmp006.h   TI IR Thermopile Sensor
 */
#ifndef TMP006_H_
#define TMP006_H_

#include <stdint.h>
#include <stdbool.h>

bool tmp006_init(void);
bool tmp006_start_temp_conversion(void);

#endif /* TMP006_H_ */
