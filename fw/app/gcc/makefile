#------------------------------------------------------------------------------
#  Temperature Server for Nordic nRF51 PCA10028 evaluation boards 
#------------------------------------------------------------------------------

TARGET_BOARD         := BOARD_PCA10028

DBGLOG_SUPPORT       := "yes"
PROVISION_DFU        := "yes"
INDICATE_CONNECTED   := "no"

#------------------------------------------------------------------------------
# Define relative paths to SDK components
#------------------------------------------------------------------------------

SDK_BASE      = ../../../..
COMPONENTS    = $(SDK_BASE)/components
TEMPLATE_PATH = $(COMPONENTS)/toolchain/gcc

OUTPUT_NAME   = application
VERSION_NAME  = $(OUTPUT_NAME)_$(BUILD_TYPE)_$(TIMESTAMP)

#------------------------------------------------------------------------------
# Proceed cautiously beyond this point.  Little should change.
#------------------------------------------------------------------------------

export OUTPUT_NAME
export GNU_INSTALL_ROOT

MAKEFILE_NAME := $(MAKEFILE_LIST)
MAKEFILE_DIR := $(dir $(MAKEFILE_NAME) ) 

LINKER_SCRIPT := ./gcc_nrf51_s110_$(TARGET_SOC).ld

MAKEFILE_NAME := $(MAKEFILE_LIST)
MAKEFILE_DIR := $(dir $(MAKEFILE_NAME) ) 

ifeq ($(TARGET_BOARD), BOARD_PCA10028)
  TARGET_SOC = xxac
else
  ECHO "invalid TARGET_BOARD"
endif

ifeq ($(OS),Windows_NT)
  include $(TEMPLATE_PATH)/Makefile.windows
else
  include $(TEMPLATE_PATH)/Makefile.posix
endif

ifeq ($(OS),Windows_NT)
    EXT = .exe
else
    UNAME_S := $(shell uname -s)
    ifeq ($(UNAME_S),Linux)
        EXT =
    endif
    ifeq ($(UNAME_S),Darwin)
        EXT = .osx
    endif
endif

# echo suspend
ifeq ("$(VERBOSE)","1")
  NO_ECHO :=
else
  NO_ECHO := @
endif

# Toolchain commands
CC       := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-gcc"
AS       := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-as"
AR       := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-ar" -r
LD       := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-ld"
NM       := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-nm"
OBJDUMP  := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-objdump"
OBJCOPY  := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-objcopy"
SIZE     := "$(GNU_INSTALL_ROOT)/bin/$(GNU_PREFIX)-size"
MK       := mkdir
RM       := rm -rf
CP       := cp
GENDAT   := ./gen_dat$(EXT)
GENZIP   := zip

BUILDMETRICS  := ./buildmetrics.py

# function for removing duplicates in a list
remduplicates = $(strip $(if $1,$(firstword $1) $(call remduplicates,$(filter-out $(firstword $1),$1))))

# source common to all targets

C_SOURCE_FILES += ../main.c
C_SOURCE_FILES += ../ble_temp.c
C_SOURCE_FILES += ../tmp006.c
C_SOURCE_FILES += ../printf.c

ifeq ($(DBGLOG_SUPPORT), "yes")
	CFLAGS += -D DBGLOG_SUPPORT=1
	C_SOURCE_FILES += ../uart.c
endif

ifeq ($(INDICATE_CONNECTED), "yes")
	CFLAGS += -D INDICATE_CONNECTED=1
endif

ifeq ($(PROVISION_DFU), "yes")
	CFLAGS += -D BLE_DFU_APP_SUPPORT

	C_SOURCE_FILES += ../dfu_trigger/ble_dfu.c
	C_SOURCE_FILES += ../dfu_trigger/bootloader_util_gcc.c
	C_SOURCE_FILES += ../dfu_trigger/dfu_app_handler.c

	INC_PATHS += -I../dfu_trigger

	LINKER_SCRIPT = ./gcc_nrf51_s110_xxac_dfu.ld
else
	LINKER_SCRIPT = ./gcc_nrf51_s110_xxac.ld
endif

C_SOURCE_FILES += $(COMPONENTS)/ble/ble_radio_notification/ble_radio_notification.c
C_SOURCE_FILES += $(COMPONENTS)/ble/ble_debug_assert_handler/ble_debug_assert_handler.c
C_SOURCE_FILES += $(COMPONENTS)/ble/ble_services/ble_dis/ble_dis.c
C_SOURCE_FILES += $(COMPONENTS)/ble/ble_services/ble_bas/ble_bas.c
C_SOURCE_FILES += $(COMPONENTS)/ble/common/ble_conn_params.c
C_SOURCE_FILES += $(COMPONENTS)/ble/common/ble_advdata.c
C_SOURCE_FILES += $(COMPONENTS)/ble/common/ble_srv_common.c
C_SOURCE_FILES += $(COMPONENTS)/ble/device_manager/device_manager_peripheral.c
C_SOURCE_FILES += $(COMPONENTS)/drivers_nrf/pstorage/pstorage.c
C_SOURCE_FILES += $(COMPONENTS)/drivers_nrf/twi_master/twi_hw_master.c
C_SOURCE_FILES += $(COMPONENTS)/drivers_nrf/hal/nrf_delay.c
C_SOURCE_FILES += $(COMPONENTS)/drivers_nrf/hal/nrf_adc.c
C_SOURCE_FILES += $(COMPONENTS)/libraries/timer/app_timer.c
C_SOURCE_FILES += $(COMPONENTS)/libraries/gpiote/app_gpiote.c
C_SOURCE_FILES += $(COMPONENTS)/libraries/scheduler/app_scheduler.c
C_SOURCE_FILES += $(COMPONENTS)/libraries/sensorsim/sensorsim.c
C_SOURCE_FILES += $(COMPONENTS)/softdevice/common/softdevice_handler/softdevice_handler.c
C_SOURCE_FILES += $(COMPONENTS)/toolchain/system_nrf51.c

# assembly files common to all targets
ASM_SOURCE_FILES  += $(COMPONENTS)/toolchain/gcc/gcc_startup_nrf51.s

# includes common to all targets
INC_PATHS += -I../
INC_PATHS += -I../../bsp/
INC_PATHS += -I$(COMPONENTS)/toolchain/gcc
INC_PATHS += -I$(COMPONENTS)/toolchain
INC_PATHS += -I$(COMPONENTS)/libraries/sensorsim
INC_PATHS += -I$(COMPONENTS)/ble/ble_services/ble_dis
INC_PATHS += -I$(COMPONENTS)/ble/ble_services/ble_bas
INC_PATHS += -I$(COMPONENTS)/softdevice/s110/headers
INC_PATHS += -I$(COMPONENTS)/ble/common
INC_PATHS += -I$(COMPONENTS)/ble/ble_error_log
INC_PATHS += -I$(COMPONENTS)/ble/ble_debug_assert_handler
INC_PATHS += -I$(COMPONENTS)/drivers_nrf/ble_flash
INC_PATHS += -I$(COMPONENTS)/drivers_nrf/twi_master
INC_PATHS += -I$(COMPONENTS)/ble/ble_radio_notification
INC_PATHS += -I$(COMPONENTS)/libraries/timer
INC_PATHS += -I$(COMPONENTS)/libraries/gpiote
INC_PATHS += -I$(COMPONENTS)/drivers_nrf/hal
INC_PATHS += -I$(COMPONENTS)/softdevice/common/softdevice_handler
INC_PATHS += -I$(COMPONENTS)/libraries/scheduler
INC_PATHS += -I$(COMPONENTS)/libraries/util
INC_PATHS += -I$(COMPONENTS)/drivers_nrf/pstorage
INC_PATHS += -I$(COMPONENTS)/drivers_nrf/pstorage/config
INC_PATHS += -I$(COMPONENTS)/libraries/util
INC_PATHS += -I$(COMPONENTS)/device
INC_PATHS += -I$(COMPONENTS)/ble/device_manager
INC_PATHS += -I$(COMPONENTS)/ble/device_manager/config
INC_PATHS += -I$(COMPONENTS)/libraries/trace


OBJECT_DIRECTORY = _build
LISTING_DIRECTORY = $(OBJECT_DIRECTORY)
OUTPUT_BINARY_DIRECTORY = $(OBJECT_DIRECTORY)

# Sorting removes duplicates
BUILD_DIRECTORIES := $(sort $(OBJECT_DIRECTORY) $(OUTPUT_BINARY_DIRECTORY) $(LISTING_DIRECTORY) )

ifeq ($(MAKECMDGOALS),debug)
  BUILD_TYPE := debug
endif

ifeq ($(BUILD_TYPE),debug)
  DEBUG_FLAGS += -D DEBUG -g -O0
  CFLAGS += -Wa,-adhln
else
  BUILD_TYPE := release
  DEBUG_FLAGS += -D NDEBUG -O3
  DBGLOG_SUPPORT = "no"
endif

# flags common to all targets
#CFLAGS += -save-temps
CFLAGS += $(DEBUG_FLAGS)
CFLAGS += -D NRF51
CFLAGS += -D BLE_STACK_SUPPORT_REQD
CFLAGS += -D S110
CFLAGS += -D SOFTDEVICE_PRESENT
CFLAGS += -D $(TARGET_BOARD)
CFLAGS += -mcpu=cortex-m0
CFLAGS += -mthumb -mabi=aapcs --std=gnu99
CFLAGS += -Wall -Werror
CFLAGS += -Wa,-adhln
CFLAGS += -mfloat-abi=soft
CFLAGS += -ffunction-sections 
CFLAGS += -fdata-sections -fno-strict-aliasing
CFLAGS += -fno-builtin

LDFLAGS += -Xlinker -Map=$(LISTING_DIRECTORY)/$(OUTPUT_NAME).map
LDFLAGS += -mthumb -mabi=aapcs -L $(TEMPLATE_PATH) -T$(LINKER_SCRIPT)
LDFLAGS += -mcpu=cortex-m0
LDFLAGS += $(DEBUG_FLAGS)
LDFLAGS += -Wl,--gc-sections
LDFLAGS += --specs=nano.specs -lc -lnosys

ASMFLAGS += $(DEBUG_FLAGS)
ASMFLAGS += -x assembler-with-cpp
ASMFLAGS += -D NRF51
ASMFLAGS += -D BLE_STACK_SUPPORT_REQD
ASMFLAGS += -D S110
ASMFLAGS += -D SOFTDEVICE_PRESENT
ASMFLAGS += -D $(TARGET_BOARD)

C_SOURCE_FILE_NAMES = $(notdir $(C_SOURCE_FILES))
C_PATHS = $(call remduplicates, $(dir $(C_SOURCE_FILES) ) )
C_OBJECTS = $(addprefix $(OBJECT_DIRECTORY)/, $(C_SOURCE_FILE_NAMES:.c=.o) )

ASM_SOURCE_FILE_NAMES = $(notdir $(ASM_SOURCE_FILES))
ASM_PATHS = $(call remduplicates, $(dir $(ASM_SOURCE_FILES) ))
ASM_OBJECTS = $(addprefix $(OBJECT_DIRECTORY)/, $(ASM_SOURCE_FILE_NAMES:.s=.o) )

TOOLCHAIN_BASE = $(basename $(notdir $(GNU_INSTALL_ROOT)))

TIMESTAMP = $(shell date +'%s')

vpath %.c $(C_PATHS)
vpath %.s $(ASM_PATHS)

OBJECTS = $(C_OBJECTS) $(ASM_OBJECTS)

all: $(BUILD_DIRECTORIES) $(OBJECTS)
	@echo Linking target: $(OUTPUT_NAME).elf
	$(NO_ECHO)$(CC) $(LDFLAGS) $(OBJECTS) $(LIBS) -o $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf
	$(NO_ECHO)$(MAKE) -f $(MAKEFILE_NAME) -C $(MAKEFILE_DIR) -e finalize

	$(NO_ECHO)$(CP) $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).bin $(OUTPUT_BINARY_DIRECTORY)/$(VERSION_NAME).bin
	$(NO_ECHO)$(CP) $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).dat $(OUTPUT_BINARY_DIRECTORY)/$(VERSION_NAME).dat
	$(NO_ECHO)$(CP) $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).zip $(OUTPUT_BINARY_DIRECTORY)/$(VERSION_NAME).zip

	@echo "*****************************************************"
	@echo "build project: $(OUTPUT_NAME)"
	@echo "build type:    $(BUILD_TYPE)"
	@echo "build with:    $(TOOLCHAIN_BASE)"
	@echo "build target:  $(TARGET_BOARD)"
	@echo "build SOC:     $(TARGET_SOC)"
	@echo "build options  --"
	@echo "               DBGLOG_SUPPORT     $(DBGLOG_SUPPORT)"
	@echo "               PROVISION_DFU      $(PROVISION_DFU)"
	@echo "               INDICATE_CONNECTED $(INDICATE_CONNECTED)"
	@echo "build products --"
	@echo "               $(OUTPUT_NAME).elf"
	@echo "               $(OUTPUT_NAME).hex"
	@echo "               $(OUTPUT_NAME).bin"
	@echo "               $(OUTPUT_NAME).dat"
	@echo "               $(OUTPUT_NAME).zip"
	@echo "build versioning --"
	@echo "               $(VERSION_NAME).bin"
	@echo "               $(VERSION_NAME).dat"
	@echo "               $(VERSION_NAME).zip"
	@echo "*****************************************************"

debug : all

release : all

# Create build directories
$(BUILD_DIRECTORIES):
	echo $(MAKEFILE_NAME)
	$(MK) $@

# Create objects from C SRC files
$(OBJECT_DIRECTORY)/%.o: %.c
	@echo Compiling file: $(notdir $<)
	$(NO_ECHO)$(CC) $(CFLAGS) $(INC_PATHS) \
	-c $< -o $@ > $(OUTPUT_BINARY_DIRECTORY)/$*.lst

# Assemble files
$(OBJECT_DIRECTORY)/%.o: %.s
	@echo Compiling file: $(notdir $<)
	$(NO_ECHO)$(CC) $(ASMFLAGS) $(INC_PATHS) -c -o $@ $<

# Link
$(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf: $(BUILD_DIRECTORIES) $(OBJECTS)
	@echo Linking target: $(OUTPUT_NAME).elf
	$(NO_ECHO)$(CC) $(LDFLAGS) $(OBJECTS) $(LIBS) -o $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf

# Create binary .bin file from the .elf file
$(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).bin: $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf
	@echo Preparing: $(OUTPUT_NAME).bin
	$(NO_ECHO)$(OBJCOPY) -O binary $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).bin

# Create binary .hex file from the .elf file
$(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).hex: $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf
	@echo Preparing: $(OUTPUT_NAME).hex
	$(NO_ECHO)$(OBJCOPY) -O ihex $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).hex

finalize: genbin genhex gendat genzip echosize memory_report

# Analyze elf file's idea of memory usage
memory_report: $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf
	-@echo ""
	-@$(BUILDMETRICS) \
		$(LINKER_SCRIPT) \
		$(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf
	-@echo ""

genbin:
	@echo Preparing: $(OUTPUT_NAME).bin
	$(NO_ECHO)$(OBJCOPY) -O binary $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).bin

# Create binary .hex file from the .elf file
genhex: 
	@echo Preparing: $(OUTPUT_NAME).hex
	$(NO_ECHO)$(OBJCOPY) -O ihex $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).hex

# Create .dat file from the .bin file
gendat: 
	@echo Preparing: $(OUTPUT_NAME).dat
	$(NO_ECHO)$(GENDAT) $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).bin $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).dat

# Create .zip file from the .bin + .dat files
genzip: 
	@echo Preparing: $(OUTPUT_NAME).zip
	-@$(GENZIP) -j $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).zip $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).bin $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).dat

echosize:
	-@echo ""
	$(NO_ECHO)$(SIZE) $(OUTPUT_BINARY_DIRECTORY)/$(OUTPUT_NAME).elf
	-@echo ""

clean:
	$(RM) $(BUILD_DIRECTORIES)

cleanobj:
	$(RM) $(BUILD_DIRECTORIES)/*.o

