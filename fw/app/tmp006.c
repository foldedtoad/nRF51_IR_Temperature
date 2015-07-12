/*
 *   tmp006.c   TI IR Thermopile Sensor
 */
#include <string.h>
#include <stdio.h>

#include "nrf_error.h"
#include "nrf_gpio.h"
#include "nrf_delay.h"
#include "app_gpiote.h"
#include "twi_master.h"
#include "softdevice_handler.h"
#include "app_scheduler.h"

#include "config.h"
#include "boards.h"
#include "tmp006.h"
#include "ble_temp.h"
#include "dbglog.h"

#include "math.h"
#include "limits.h"


extern ble_temp_t  g_temp_service;  // see main.c

/*---------------------------------------------------------------------------*/
/* Configuration section                                                     */
/*---------------------------------------------------------------------------*/

#define I2C_SCL                 TMP006_SCL_PIN
#define I2C_SDA                 TMP006_SDA_PIN
#define I2C_nDRDY               TMP006_INTERRUPT_PIN

//#define TMP006_BUS_ADDRESS      0x8A
#define TMP006_BUS_ADDRESS      0x80

/* Device Registers */
#define TMP006_VOLT_REG        0x00
#define TMP006_TEMP_REG        0x01
#define TMP006_CONFIG_REG      0x02
#define TMP006_MANUFACT_REG    0xFE
#define TMP006_DEVICE_ID_REG   0xFF

#define TMP006_CONFIG_RST      0x8000
#define TMP006_CONFIG_MOD      0x7000
#define TMP006_CONFIG_PWRDWN   0x0000
#define TMP006_CONFIG_CR_4     0x0100
#define TMP006_CONFIG_CR_2     0x0300
#define TMP006_CONFIG_CR_1     0x0500
#define TMP006_CONFIG_CR_0p5   0x0700
#define TMP006_CONFIG_CR_0p25  0x0F00
#define TMP006_CONFIG_EN       0x0100
#define TMP006_CONFIG_DRDY     0x0080

#define TMP006_ONESHOT_MODE    0x01
#define TMP006_CONVERSION_DONE 0x80

#define TMP006_VENDOR_ID       0x5449     // "TI"
#define TMP006_DEVICE_ID       0x0067

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/

static bool Initialized = false;

static uint16_t  device_id;

static app_gpiote_user_id_t   tmp006_gpiote_user_id;

static bool tmp006_irq_disable(void);
static bool tmp006_irq_enable(void);

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static void tmp006_twi_enable(void)
{
    NRF_TWI1->ENABLE = TWI_ENABLE_ENABLE_Enabled << TWI_ENABLE_ENABLE_Pos;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static void tmp006_twi_disable(void)
{
    NRF_TWI1->ENABLE = TWI_ENABLE_ENABLE_Disabled << TWI_ENABLE_ENABLE_Pos;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_powerdown(void)
{
    bool     success;
    uint16_t config;
    uint8_t  data[3];

    config = TMP006_CONFIG_PWRDWN;

    data[0] = TMP006_CONFIG_REG;
    data[1] = (uint8_t) ((config >> 8) & 0x00FF);
    data[2] = (uint8_t) ((config >> 0) & 0x00FF);

    success = twi_master_transfer(TMP006_BUS_ADDRESS,
                                  (uint8_t*)&data, sizeof(data),
                                  TWI_ISSUE_STOP);
    return success;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_id_read(uint16_t * id)
{
    bool     success = false;
    uint8_t  reg = TMP006_DEVICE_ID_REG;
    uint8_t  data[2];

    success = twi_master_transfer(TMP006_BUS_ADDRESS,
                                  &reg, sizeof(reg),
                                  TWI_DONT_ISSUE_STOP);
    if (success == true) {

        success = twi_master_transfer(TMP006_BUS_ADDRESS | TWI_READ_BIT,
                                     (uint8_t*)&data, sizeof(data),
                                     TWI_ISSUE_STOP);
        if (success == true) {
            *id = (data[0] * 256) + data[1];
        }
    } 
    return success;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static uint16_t tmp006_config_read(void)
{
    bool     success;
    uint8_t  reg = TMP006_CONFIG_REG;
    uint8_t  data[2];
    uint16_t config;

    success = twi_master_transfer(TMP006_BUS_ADDRESS,
                                  &reg, sizeof(reg),
                                  TWI_DONT_ISSUE_STOP);
    if (success == true) {

        success = twi_master_transfer(TMP006_BUS_ADDRESS | TWI_READ_BIT,
                                      (uint8_t*)&data, sizeof(data),
                                      TWI_ISSUE_STOP);
        if (success == false)
            config = 0;  // read failed
        else
            config = (data[0] * 256) + data[1];
    }
    else
        config = 0; // read failed

    return config;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
bool tmp006_start_temp_conversion(void)
{
    bool     success;
    uint16_t config;
    uint8_t  data[3];

    tmp006_twi_enable();
    tmp006_irq_enable();

    config = TMP006_CONFIG_MOD | TMP006_CONFIG_CR_4;

    data[0] = TMP006_CONFIG_REG;
    data[1] = (uint8_t) ((config >> 8) & 0x00FF);
    data[2] = (uint8_t) ((config >> 0) & 0x00FF);
 
    success = twi_master_transfer(TMP006_BUS_ADDRESS,
                                  (uint8_t*)&data, sizeof(data),
                                  TWI_ISSUE_STOP);
    return success;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_read_Vobj(int16_t * Vobj)
{
    bool     success;
    uint8_t  reg = TMP006_VOLT_REG;
    uint8_t  data[2];

    success = twi_master_transfer(TMP006_BUS_ADDRESS,
                                  &reg, sizeof(reg),
                                  TWI_DONT_ISSUE_STOP);
    if (success == true) {

        success = twi_master_transfer(TMP006_BUS_ADDRESS | TWI_READ_BIT,
                                     (uint8_t*)&data, sizeof(data),
                                     TWI_ISSUE_STOP);
        if (success == true) {

            *Vobj = ((data[0] * 256) + data[1]);
            //PRINTF("Vobj: %0d\n", *Vobj);
            return true;
        }
    }

    PUTS("tmp006_read_Vobj failed");

    return false;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_read_Tdie(int16_t * Tdie)
{
    bool     success;
    uint8_t  reg = TMP006_TEMP_REG;
    uint8_t  data[2];

    success = twi_master_transfer(TMP006_BUS_ADDRESS,
                                  &reg, sizeof(reg),
                                  TWI_DONT_ISSUE_STOP);
    if (success == true) {

        success = twi_master_transfer(TMP006_BUS_ADDRESS | TWI_READ_BIT,
                                     (uint8_t*)&data, sizeof(data),
                                     TWI_ISSUE_STOP);
        if (success == true) {

            *Tdie = ((data[0] * 256) + data[1]);
            //PRINTF("Tdie: %0d\n", *Tdie);
            return true;
        }
    }

    PUTS("tmp006_read_Tdie failed");

    return false;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static void tmp006_Convert(int16_t rawVolt,
                           int16_t rawTemp,
                           float  * tObj,
                           float  * tAmb)
{
    /*
     *  Calculate die temperature [°C]
     */
    *tAmb = (double)((int16_t)rawTemp)/128.0;

    /*
     *  Calculate target temperature [°C]
     */
    double Vobj2 = (double)(int16_t)rawVolt;
    Vobj2 *= 0.00000015625;

    double Tdie2 = *tAmb + 273.15;
    const double S0 = 6.4E-14;      /* Calibration factor */

    const double a1 = 1.75E-3;
    const double a2 = -1.678E-5;
    const double b0 = -2.94E-5;
    const double b1 = -5.7E-7;
    const double b2 = 4.63E-9;
    const double c2 = 13.4;
    const double Tref = 298.15;

    double S = S0 * (1+a1*(Tdie2 - Tref) + a2*pow((Tdie2 - Tref),2));
    double Vos = b0 + b1*(Tdie2 - Tref) + b2*pow((Tdie2 - Tref),2);
    double fObj = (Vobj2 - Vos) + c2*pow((Vobj2 - Vos),2);
    double t = pow(pow(Tdie2,4) + (fObj/S),.25);

    *tObj = (t - 273.15);
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_temp_read(int16_t * temperature)
{
    int16_t Vobj;
    int16_t Tdie;
    float    tObj;
    float    tAmb;

    do {
        if (tmp006_read_Vobj(&Vobj) == false) break;
        if (tmp006_read_Tdie(&Tdie) == false) break;

        tmp006_powerdown();

        tmp006_Convert(Vobj, Tdie, &tObj, &tAmb); 

        if ((tObj < LONG_MIN-0.5) || (tObj > LONG_MAX+0.5 )) break;

        *temperature = (uint16_t)(tObj >= 0) ? (long)(tObj+0.5) : (long)(tObj-0.5);

        return true;

    } while (0);

    tmp006_powerdown();
    return false;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static void tmp006_execute(void * p_event_data, uint16_t event_size)
{
    int16_t  temperature = 0;

    tmp006_temp_read(&temperature);

    tmp006_twi_disable();
    tmp006_irq_disable();

    PRINTF("TMP006: temp  %d C\n", (int) temperature);

    ble_temp_on_temperature_update(&g_temp_service, &temperature);
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static void tmp006_event_handler(uint32_t low_to_high, uint32_t high_to_low)
{
    app_sched_event_put(NULL, 0, tmp006_execute);
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_interrupt_config(void)
{
    uint32_t status;

    /*
     *  Register as a GPIOTE user.
     *  Set interrupts to trigger on Falling-Edge of I2C_nDRDY line.
     */
    status = app_gpiote_user_register(&tmp006_gpiote_user_id,
                                      0,                           // low_to_high
                                      (1 << TMP006_INTERRUPT_PIN), // high_to_low
                                      tmp006_event_handler);
    if (status != NRF_SUCCESS) {
        PUTS("gpiote register failed");
        return false;
    }

    return true;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_irq_enable(void)
{
    uint32_t status;

    status = app_gpiote_user_enable(tmp006_gpiote_user_id);
    if (status != NRF_SUCCESS) {
        return false;
    }

    return true;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static bool tmp006_irq_disable(void)
{
    uint32_t status;

    status = app_gpiote_user_disable(tmp006_gpiote_user_id);
    if (status != NRF_SUCCESS)
        return false;

    return true;
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
static void tmp006_gpio_config(void)
{
    /* I2C_SCL - Input */
    nrf_gpio_pin_dir_set(I2C_SCL, NRF_GPIO_PIN_DIR_INPUT);

    /* I2C_SDA - Output high */
    nrf_gpio_pin_dir_set(I2C_SDA, NRF_GPIO_PIN_DIR_OUTPUT);
    nrf_gpio_pin_set(I2C_SDA);

    /* I2C_nDRDY - Input Interrupt */
    nrf_gpio_pin_dir_set(I2C_nDRDY, NRF_GPIO_PIN_DIR_INPUT);
    nrf_gpio_cfg_input(I2C_nDRDY, NRF_GPIO_PIN_NOPULL);
}

/*---------------------------------------------------------------------------*/
/*                                                                           */
/*---------------------------------------------------------------------------*/
bool tmp006_init(void)
{
    Initialized = false;

    PUTS(__FUNCTION__);

    /* Configure I2C and Interrupt lines for IO */
    tmp006_gpio_config();

    if (twi_master_init() == false) {
        PUTS("twi_master_init failed");
        return false;
    }

    /* Configure Device for interrupts */
    tmp006_interrupt_config();

    /* Enable Interrupts */
    tmp006_irq_enable();

    /* Delay a bit to allow tmp006 to intialize. */
    nrf_delay_ms(1);

    /* Read Device Id register */
    tmp006_id_read(&device_id);

    if (device_id != TMP006_DEVICE_ID) {
        PRINTF("TMP006: wrong device id: 0x%04X\n", (unsigned) device_id);
    }

    /* Disable TWI until needed */
    tmp006_powerdown();
    tmp006_irq_disable();
    tmp006_twi_disable();

    Initialized = true;

    return Initialized;
}
