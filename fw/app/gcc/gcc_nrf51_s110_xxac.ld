/* Linker script to configure memory regions. */

SEARCH_DIR(.)
GROUP(-lgcc -lc -lnosys -lm)

MEMORY
{
  FLASH (rx) : ORIGIN = 0x00018000, LENGTH = 160K 
  RAM (rwx) :  ORIGIN = 0x20002000, LENGTH = 24K 
}


INCLUDE "gcc_nrf51_common.ld"
