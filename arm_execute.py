import math
import random
import re

# <opcode>{<cond>}{S} <Rd>, <Rn>, <shifter_operand>
# <opcode>{<cond>}{Br} <branch_immediate>

tests = open("RegisterFileARM.txt", 'w')
tests.write("@ This is an execution suite for a pseudo ARMv7 cpu made using logisim @\n")

DataProcessing = {
    0: "AND", 1: "EOR", 2: "SUB", 3: "RSB", 4: "ADD", 5: "ADC", 6: "SBC", 7: "RSC",
    8: "TST", 9: "TEQ", 10: "CMP", 11: "CMN", 12: "ORR", 13: "MOV", 14: "BIC", 15: "MVN"
}

Shifts = {
    0: "LSL", 1: "LSR", 2: "ASR", 3: "ROR", 4: "RRX"
}

DataTransfer = {
    0: "LDR", 1: "LDRB", 2: "LDRSB", 3: "STR", 4: "STRB"
}

Branches = {
    0: "B", 1: "BL", 2: "BX", 3: "BLX"
}

Conditionals = {
    0: "EQ", 1: "NE", 2: "HS", 3: "LO", 4: "MI", 5: "PL", 6: "VS", 7: "VC",
    8: "HI", 9: "LS", 10: "GE", 11: "LT", 12: "GT", 13: "LE", 14: "AL", 15: "NV"
}

Registers = {
    0: "a1", 1: "a2", 2: "a3", 3: "a4", 4: "v1", 5: "v2", 6: "v3", 7: "v4",
    8: "v5", 9: "v6", 10: "v7", 11: "v8", 12: "ip", 13: "sp", 14: "lr", 15: "pc",
    16: "cpsr"
}

"""  """
def compileInstruction():
    

""" """
def executeInstruction(num):
    while i < num:
        # Updates Register File and RAM Memory files
        i += 1

""" """
def showRegisters():


""" """
def showRAMAccess():



tests.close()

""" 
## Data Processing ##

31 28   27  26  25  24  21  20  19 16  15 12    11       8  7     0
cond    0   0   1   opcode  S   Rn     Rd       rotate_imm  immed_8

31 28   27  26  25  24 21   20  19 16   15 12   11      7   6   5   4   3 0
cond    0   0   0   opcode  S   Rn      Rd      shift_imm   shift   0   Rm

31 28   27  26  25  24 21   20  19 16   15 12   11 8    7   6   5   4   3 0
cond    0   0   0   opcode  S   Rn      Rd      Rs      0   shift   1   Rm 

## Data Transfer ##


"""