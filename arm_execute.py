import math
import random
import re

# <opcode>{<cond>}{S} <Rd>, <Rn>, <shifter_operand>
# <opcode>{<cond>}{Br} <branch_immediate>

file = open("TestARM.txt", 'r')

def fetch(line):
    """ Fetches and sends the first line of the TestArm.txt file to
    the next stage of the pipeline. Requires formatting and a few
    other commands to get it into a string format."""
    print "fetched"


def decode(line):
    """  """


def execute(line):
    """ """
    return 0


def memory(line):
    """ """
    return 0


def writeback(line):  
    """ """
    return 0


def run(inst):
    """ """
    fetch(line)
    decode(line)
    execute(line)
    memory(line)
    writeback(line)
    update()


def update():
    """ updates registers and memory access files """


def showRegisters():
    """ """
    return 0


def showRAMAccess():
    """ """
    return 0


# Scripts
for line in file:
    run(line)

file.close()

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