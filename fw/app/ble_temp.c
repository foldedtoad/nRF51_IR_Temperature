/* 
 *  ble_temp.c   BLE surface for TMP006 Infrared Thermopile Sensor access.
 *  Copyright (c) 2015 Robin Callender. All Rights Reserved.
 */
#include <stdio.h>
#include <string.h>

#include "nordic_common.h"
#include "ble_srv_common.h"
#include "app_util.h"
#include "app_timer.h"
#include "app_timer_appsh.h"

#include "config.h"
#include "ble_temp.h"
#include "tmp006.h"
#include "dbglog.h"
#include "ble_dfu.h"

static app_timer_id_t   m_temperature_timer_id;   /* Temperature interval timer. */

static uint32_t         m_interval = DEFAULT_TEMPERATURE_MEASURE_INTERVAL;
static uint32_t         m_interval_ticks;

/* 
 *  Function for handling the Connect event.
 *
 *  param[in]   p_temp      Temp structure.
 *  param[in]   p_ble_evt   Event received from the BLE stack.
 */
static void on_connect(ble_temp_t * p_temp, ble_evt_t * p_ble_evt)
{
    p_temp->conn_handle = p_ble_evt->evt.gap_evt.conn_handle;
}

/*
 *  Function for handling the Disconnect event.
 *
 *  param[in]   p_temp      Temp structure.
 *  param[in]   p_ble_evt   Event received from the BLE stack.
 */
static void on_disconnect(ble_temp_t * p_temp, ble_evt_t * p_ble_evt)
{
    UNUSED_PARAMETER(p_ble_evt);
    p_temp->conn_handle = BLE_CONN_HANDLE_INVALID;
    p_temp->notify_enabled = false;
}

/** Function for handling write events to the Temperature characteristic.
 *
 *  param[in]   p_evt_write   Write event received from the BLE stack.
 */
static void on_temp_cccd_write(ble_temp_t * p_temp, ble_gatts_evt_write_t * p_evt_write)
{
    bool enabled = ble_srv_is_notification_enabled(p_evt_write->data);

    if (p_evt_write->len == 2) {
        if (enabled) {
            p_temp->notify_enabled = true;
            PUTS("Start temp timer");
            APP_ERROR_CHECK(app_timer_start(m_temperature_timer_id, m_interval, NULL));
        }
        else {
            p_temp->notify_enabled = false;
            PUTS("Stop temp timer");
            APP_ERROR_CHECK(app_timer_stop(m_temperature_timer_id));
        }
    }
}

/** Function for handling write events to the Interval characteristic.
 *
 *  param[in]   p_evt_write   Write event received from the BLE stack.
 */
static void on_temp_interval_write(ble_temp_t * p_temp, ble_gatts_evt_write_t * p_evt_write)
{
    uint32_t value;

    if (p_evt_write->len == sizeof(m_interval)) {

        value = *(uint32_t*) p_evt_write->data;

        PRINTF("Update Interval value: %u\n",(unsigned) value);

        if ((value >= 1) || (value > 86400)) {  // 86400 == seconds per day

            m_interval = value;
            m_interval_ticks = app_timer_ticks(m_interval * 1000 /* 1000 ms */);

            APP_ERROR_CHECK(app_timer_stop(m_temperature_timer_id));
            APP_ERROR_CHECK(app_timer_start(m_temperature_timer_id, m_interval_ticks, NULL));
        }
        else {
            PUTS("Interval value invalid");
        }
    }
}

/*
 *  Function for handling the Write event.
 *
 *  param[in]   p_temp      Temp structure.
 *  param[in]   p_ble_evt   Event received from the BLE stack.
 */
static void on_write(ble_temp_t * p_temp, ble_evt_t * p_ble_evt)
{
    ble_gatts_evt_write_t  * p_evt_write = &p_ble_evt->evt.gatts_evt.params.write;

    switch (p_evt_write->context.char_uuid.uuid) {

        case TEMP_UUID_TEMPERATURE_CHAR:
            if (p_evt_write->len != sizeof(uint16_t)) {
                //printf("on_write: bad len: %d\n", p_evt_write->len);
                break;
            }
            on_temp_cccd_write(p_temp, p_evt_write);
            break;

        case TEMP_UUID_INTERVAL_CHAR:
            if (p_evt_write->len != sizeof(uint32_t)) {
                //printf("on_write: bad len: %d\n", p_evt_write->len);
                break;
            }
            on_temp_interval_write(p_temp, p_evt_write);
            break;

        case TEMP_UUID_SERVICE:
        case BLE_UUID_BATTERY_LEVEL_CHAR:
        case BLE_UUID_GATT_CHARACTERISTIC_SERVICE_CHANGED:
        case BLE_DFU_CTRL_PT_UUID:
            // Quietly ignore these events.
            break;

        default:
            printf("unknown uuid: 0x%x\n",
                   (unsigned) p_evt_write->context.char_uuid.uuid);
            break;
    }  
}


/*
 *  Function for handling the BLE events.
 *
 *  param[in]   p_temp      Temp Service structure.
 *  param[in]   p_ble_evt   Event received from the BLE stack.
 */
void ble_temp_on_ble_evt(ble_temp_t * p_temp, ble_evt_t * p_ble_evt)
{
    switch (p_ble_evt->header.evt_id) {

        case BLE_GAP_EVT_CONNECTED:
            PUTS("evt_id: CONNECTED");
            on_connect(p_temp, p_ble_evt);
            break;

        case BLE_GAP_EVT_DISCONNECTED:
            on_disconnect(p_temp, p_ble_evt);
            PUTS("Stop temp timer");
            APP_ERROR_CHECK(app_timer_stop(m_temperature_timer_id));
            break;

        case BLE_GATTS_EVT_WRITE:
            on_write(p_temp, p_ble_evt);
            break;

        case BLE_GATTS_EVT_RW_AUTHORIZE_REQUEST:
            break;

        case BLE_GATTS_EVT_SYS_ATTR_MISSING:
            break;

        case BLE_GATTS_EVT_HVC:
            break;

        case BLE_GATTS_EVT_SC_CONFIRM:
            break;

        case BLE_GATTS_EVT_TIMEOUT:
            break;

        default:
            break;
    }
}


/*
 *  Function for adding the Temperature characteristic.
 */
static uint32_t temperature_char_add(ble_temp_t * p_temp)
{
    ble_gatts_char_md_t  char_md;
    ble_gatts_attr_md_t  cccd_md;
    ble_gatts_attr_t     attr_char_value;
    ble_uuid_t           ble_uuid;
    ble_gatts_attr_md_t  attr_md;
    ble_gatts_char_pf_t  char_pf;
    ble_gatts_attr_md_t  desc_md;

    static const uint8_t user_desc[] = "Temperature";

    // Setup CCCD attribute
    memset(&cccd_md, 0, sizeof(cccd_md));
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&cccd_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&cccd_md.write_perm);
    cccd_md.vloc = BLE_GATTS_VLOC_STACK;

    memset(&char_pf, 0, sizeof(char_pf));
    char_pf.format = BLE_GATT_CPF_FORMAT_UTF8S;

    // Setup User Description attribute
    memset(&desc_md, 0, sizeof(desc_md));
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&desc_md.read_perm);
    desc_md.vloc = BLE_GATTS_VLOC_STACK;

    // Temp Service Characterstic
    memset(&char_md, 0, sizeof(char_md));
    char_md.char_props.read         = 1;
    char_md.char_props.notify       = 1;
    char_md.p_char_user_desc        = (uint8_t*) &user_desc;
    char_md.char_user_desc_size     = sizeof(user_desc);
    char_md.char_user_desc_max_size = sizeof(user_desc);
    char_md.p_char_pf               = &char_pf;
    char_md.p_user_desc_md          = &desc_md;
    char_md.p_cccd_md               = &cccd_md;
    char_md.p_sccd_md               = NULL;

    ble_uuid.type = p_temp->uuid_type;
    ble_uuid.uuid = TEMP_UUID_TEMPERATURE_CHAR;

    memset(&attr_md, 0, sizeof(attr_md));
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_NO_ACCESS(&attr_md.write_perm);
    attr_md.vloc       = BLE_GATTS_VLOC_STACK;
    attr_md.rd_auth    = 0;
    attr_md.wr_auth    = 0;
    attr_md.vlen       = 0;

    memset(&attr_char_value, 0, sizeof(attr_char_value));
    attr_char_value.p_uuid       = &ble_uuid;
    attr_char_value.p_attr_md    = &attr_md;
    attr_char_value.init_len     = sizeof(uint16_t);
    attr_char_value.init_offs    = 0;
    attr_char_value.max_len      = sizeof(uint16_t);
    attr_char_value.p_value      = NULL;

    return sd_ble_gatts_characteristic_add(p_temp->service_handle, 
                                           &char_md,
                                           &attr_char_value,
                                           &p_temp->temperature_char_handles);
}

/*
 *  Function for adding the Interval set/get characteristic.
 */
static uint32_t interval_char_add(ble_temp_t * p_temp)
{
    ble_gatts_char_md_t  char_md;
    ble_gatts_attr_t     attr_char_value;
    ble_uuid_t           ble_uuid;
    ble_gatts_attr_md_t  attr_md;
    ble_gatts_char_pf_t  char_pf;
    ble_gatts_attr_md_t  desc_md;

    static const uint8_t user_desc[] = "Interval";

    memset(&desc_md, 0, sizeof(desc_md));
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&desc_md.read_perm);
    desc_md.vloc = BLE_GATTS_VLOC_STACK;

    memset(&char_pf, 0, sizeof(char_pf));
    char_pf.format = BLE_GATT_CPF_FORMAT_UINT32;

    memset(&char_md, 0, sizeof(char_md));
    char_md.char_props.read         = 1;
    char_md.char_props.write        = 1;
    char_md.p_char_user_desc        = (uint8_t*) &user_desc;
    char_md.char_user_desc_size     = sizeof(user_desc);
    char_md.char_user_desc_max_size = sizeof(user_desc);
    char_md.p_char_pf               = &char_pf;
    char_md.p_user_desc_md          = &desc_md;
    char_md.p_cccd_md               = NULL;
    char_md.p_sccd_md               = NULL;

    ble_uuid.type = p_temp->uuid_type;
    ble_uuid.uuid = TEMP_UUID_INTERVAL_CHAR;

    memset(&attr_md, 0, sizeof(attr_md));
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.read_perm);
    BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.write_perm);
    attr_md.vloc       = BLE_GATTS_VLOC_USER;              // NOTE using app storage
    attr_md.rd_auth    = 0;
    attr_md.wr_auth    = 0;
    attr_md.vlen       = 0;

    memset(&attr_char_value, 0, sizeof(attr_char_value));
    attr_char_value.p_uuid       = &ble_uuid;
    attr_char_value.p_attr_md    = &attr_md;
    attr_char_value.init_len     = sizeof(m_interval);
    attr_char_value.init_offs    = 0;
    attr_char_value.max_len      = sizeof(m_interval);
    attr_char_value.p_value      = (uint8_t*) &m_interval;

    return sd_ble_gatts_characteristic_add(p_temp->service_handle,
                                           &char_md,
                                           &attr_char_value,
                                           &p_temp->interval_char_handles);
}

/*
 *
 */
static void temperature_timeout_handler(void * p_context)
{
    tmp006_start_temp_conversion();
}

/*
 *  Function for initializing Temp's BLE usage.
 */
uint32_t ble_temp_init(ble_temp_t * p_temp)
{
    uint32_t   err_code;
    ble_uuid_t ble_uuid;

    // Initialize Temperature Measurement Interval timer
    err_code = app_timer_create(&m_temperature_timer_id,
                                APP_TIMER_MODE_REPEATED,
                                temperature_timeout_handler);
    APP_ERROR_CHECK(err_code);

    // Convert default Interval value to ticks.
    m_interval_ticks = app_timer_ticks(m_interval * 1000 /* 1000 ms */);

    // Initialize service structure
    p_temp->conn_handle    = BLE_CONN_HANDLE_INVALID;
    p_temp->notify_enabled = false;

    // Add service
    ble_uuid128_t base_uuid = {TEMP_UUID_BASE};
    err_code = sd_ble_uuid_vs_add(&base_uuid, &p_temp->uuid_type);
    if (err_code != NRF_SUCCESS) {
        return err_code;
    }
    
    ble_uuid.type = p_temp->uuid_type;
    ble_uuid.uuid = TEMP_UUID_SERVICE;

    err_code = sd_ble_gatts_service_add(BLE_GATTS_SRVC_TYPE_PRIMARY, &ble_uuid, &p_temp->service_handle);
    if (err_code != NRF_SUCCESS) {
        return err_code;
    }

    err_code = temperature_char_add(p_temp);
    if (err_code != NRF_SUCCESS) {
        return err_code;
    }

    err_code = interval_char_add(p_temp);
    if (err_code != NRF_SUCCESS) {
        return err_code;
    }

    return NRF_SUCCESS;
}

uint32_t ble_temp_on_temperature_update(ble_temp_t * p_temp, int16_t * value)
{
    ble_gatts_hvx_params_t params;
    uint16_t len = sizeof(uint16_t);
    uint32_t status;

    if (p_temp->notify_enabled == false) {
        return NRF_SUCCESS;
    }

    memset(&params, 0, sizeof(params));
    params.type   = BLE_GATT_HVX_NOTIFICATION;
    params.handle = p_temp->temperature_char_handles.value_handle;
    params.p_data = (void*)value;
    params.p_len  = &len;

    status = sd_ble_gatts_hvx(p_temp->conn_handle, &params);

    return status;
}
