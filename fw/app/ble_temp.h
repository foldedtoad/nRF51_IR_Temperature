/* 
 *  Copyright (c) 2015 Robin Callender. All Rights Reserved.
 */

#ifndef BLE_TEMP_H__
#define BLE_TEMP_H__

#include <stdint.h>
#include <stdbool.h>

#include "ble.h"
#include "ble_srv_common.h"

//
//  0000XXXX-0eea-45d1-a44a-bb3ce36fced6
// 
#define TEMP_UUID_BASE {0xd6, 0xce, 0x6f, 0xe3, 0x3c, 0xbb, 0x4a, 0xa4, 0xd1, 0x45, 0xea, 0x0e, 0x00, 0x00, 0x00, 0x00}
#define TEMP_UUID_SERVICE            0xfad0
#define TEMP_UUID_TEMPERATURE_CHAR   0xfad1
#define TEMP_UUID_INTERVAL_CHAR      0xfad2

// Forward declaration of the ble_temp_t type. 
typedef struct _ble_temp   ble_temp_t;

/*  Temp Service structure. 
 *  This contains various status information for the service. 
 */
typedef struct _ble_temp {
    uint16_t                       service_handle;
    ble_gatts_char_handles_t       temperature_char_handles;
    ble_gatts_char_handles_t       interval_char_handles;
    uint8_t                        uuid_type;
    uint16_t                       conn_handle;
    bool                           notify_enabled;   
} ble_temp_t;


/* Temp Service Handle */
extern ble_temp_t  g_temp_service;


/*  Function for initializing the Temp Service.
 *
 * @param[out]  p_temp       Temp Service structure. This structure will have to be supplied by
 *                           the application. It will be initialized by this function, and will later
 *                           be used to identify this particular service instance.
 * @param[in]   p_temp_init  Information needed to initialize the service.
 *
 * @return      NRF_SUCCESS on successful initialization of service, otherwise an error code.
 */
uint32_t ble_temp_init(ble_temp_t * p_temp);

/*  Function for handling the Application's BLE Stack events.
 *  Handles all events from the BLE stack of interest to the LED Button Service.
 *
 *   param[in]   p_temp     Temp Service structure.
 *   param[in]   p_ble_evt  Event received from the BLE stack.
 */
void ble_temp_on_ble_evt(ble_temp_t * p_lbs, ble_evt_t * p_ble_evt);

/**@brief Function for sending a temperature update notification.
 */
uint32_t ble_temp_on_temperature_update(ble_temp_t * p_temp, int16_t * value);

#endif // BLE_TEMP_H__
