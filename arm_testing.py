import math
import random
import re

# <opcode>{<cond>}{S} <Rd>, <Rn>, <shifter_operand>
# <opcode>{<cond>}{Br} <branch_immediate>

# MACROS DEBUGS
hex = True # if hex == true then insert '&' in front of every immediate
neg = False # if neg == false then remove all - signs
addr = False # if addr == false then no prepost addressing
branch = False

tests = open("TestARM.txt", 'w')
tests.write("@ This is a variable testing suite for a pseudo ARMv7 cpu made using logisim @\n")
if branch == False:
    tests.write("@ No Branch instructions @\n")
tests.write("@ by Pablo and Akhil @\n")

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

""" Helper function(s) that use regular expression building to randomly 
    create a valid ARMv7 instruction using the above static dictionaries.
    Returns: a valid string instruction. """
def buildProcessing():
    instruction = DataProcessing.get(random.randint(0,15))
    if (random.randint(0,2) == 0):
        instruction += Conditionals.get(random.randint(0,14))
    instruction += "S " if (random.randint(0,1) > 0) else " "
    instruction += Registers.get(random.randint(0,11)) + ", "
    instruction += Registers.get(random.randint(0,11)) + ", "
    instruction += operandTwo_P(instruction)
    return instruction

def buildTransfer():
    # instruction = "Not implemented yet"
    instruction = DataTransfer.get(random.randint(0,4))
    if (random.randint(0,2) == 0):
        instruction += Conditionals.get(random.randint(0,14))
    instruction += "S " if (random.randint(0,1) > 0) else " "
    instruction += Registers.get(random.randint(0,11)) + ", "
    instruction += '[' + Registers.get(random.randint(0,11))
    instruction += operandTwo_T(instruction)
    return instruction

def buildBranch():
    inst = random.randint(0,3)
    instruction = Branches.get(inst)
    if (random.randint(0,2) == 0):
        instruction += Conditionals.get(random.randint(0,14))
    instruction += "S " if (random.randint(0,1) > 0) else " "
    instruction += operandTwo_B(inst)
    return instruction


""" Helper function(s) that randomly choose format or parameters 
    for instruction type. """
def chooseBuild():
    # format = random.randint(0,1)
    if branch == True:
        format = random.randint(0,2)
    else:
        format = random.randint(0,1)
    # format = 2
    switcher = {
        0 : buildProcessing,
        1 : buildTransfer,
        2 : buildBranch
    }
    # Get the function from the switcher dictionary
    func = switcher.get(format, lambda : "NOP")
    # Execute the function
    return func()


""" Helper function(s) that decide what random operand two
    suffixes should be used for their corresponding format
    types. """
def operandTwo_P(inst):
    immediate = random.randint(0,1) # if 1 then immediate
    shiftReg = random.randint(0,1) # if 1 then register shift else 5-bit shift immediate
    reg = random.randint(0,4) # if 0 then register only, else 80% of time its a shift operand
    shift = random.randint(0,3) # case(arg) then LSL, LSR, ASR, ROR, ignore RRX for now

    operand = ""
    if immediate == 1:
        if hex == True:
            operand += "#&" + str('{:03x}'.format(random.randint(-1024, 1024 - 1))) 
        else:
            operand += "#" + str(random.randint(-1024, 1024 - 1))
        # rot shift, #&imm
        return operand

    if shiftReg == 1:
        operand += Registers.get(random.randint(0,11)) + ", " 
        operand += Shifts.get(shift) + " " + Registers.get(random.randint(0,11))
        # Rm, shift type Rs
        return operand

    if reg == 0:
        operand += Registers.get(random.randint(0,11))
        # Rm
        return operand
    else:
        operand += Registers.get(random.randint(0,11)) + ", "
        if hex == True:
            operand += Shifts.get(shift) + " #&" + str('{:02x}'.format(random.randint(0,31)))
        else:
            operand += Shifts.get(shift) + " #" + str(random.randint(0,31))
        # Rm, shift type #&imm
        return operand

def operandTwo_T(inst):
    immediate = random.randint(0,1) # if 1 then immediate
    shiftReg = random.randint(0,1) # lsl barrel shifting
    reg = random.randint(0,4) # if 0 then register only, else 80% of time its a shift operand
    shift = random.randint(0,3) # case(arg) then LSL, LSR, ASR, ROR, ignore RRX for now
    
    if addr == True:
        prePost = random.randint(0,2) # if 0 then regular, if 1 then pre, if 2 then post addressing
    else: # No Pre or Post addressing, only offset
        prePost = 0

    operand = ""
    if immediate == 1:
        if hex == True:
            immediate = "#&" + str('{:03x}'.format(random.randint(-128, 128 - 1))) 
        else:
            immediate = "#" + str(random.randint(-128, 128 - 1))
        # [r0, #&imm]
        if prePost == 0:
            operand += ", "
            return operand + immediate + "]"
        elif prePost == 1:
            operand += ", "
            return operand + immediate + "]!"
        else:
            return operand + "]" + ", " + immediate 

    if shiftReg == 1:
        if prePost == 0 or prePost == 1:
            operand += ", "
            operand += Registers.get(random.randint(0,11)) + ", " 
            if hex == True:
                immediate = "#&" + str('{:03x}'.format(random.randint(0, 32 - 1))) 
            else:
                immediate = "#" + str(random.randint(0, 32))
            operand += Shifts.get(shift) + " " + immediate + "]"
            operand += "!" if (prePost == 1) else ""
            # [r0, r1, lsl #x]
        else:
            operand += "], " + Registers.get(random.randint(0,11)) + ", "
            if hex == True:
                immediate = "#&" + str('{:03x}'.format(random.randint(0, 32 - 1))) 
            else:
                immediate = "#" + str(random.randint(0, 32))
            operand += Shifts.get(shift) + " " + immediate 
        return operand

    if reg == 0:
        operand += ", "
        if hex == True:
            immediate = "#&" + str('{:03x}'.format(random.randint(-128, 128 - 1))) 
        else:
            immediate = "#" + str(random.randint(-128, 128 - 1))
        operand += Registers.get(random.randint(0,11))
        if prePost == 0:
            operand += "]"
        elif prePost == 1:
            operand += "]!"
        else:
            operand += "], " + immediate
        # [r0, r1]
        return operand
    else:
        operand += "]"
        return operand

def operandTwo_B(inst):
    operand = ""
    if inst <= 2:
        if hex == True:
            operand += "#&" + str('{:06x}'.format(random.randint(0, 2**24 - 1)))
        else:
            operand += "#" + str(random.randint(0, 2**24 - 1))
    elif inst == 3:
        operand += Registers.get(random.randint(0,11))
    return operand

    
""" Function that uses buildInstruction() to produce an array of valid
    instructions to be proccessed by the Logisim ARMv7 CPU"""
def printInstruction(n):
    i = 0
    while i < n:
     # Prints all instructions to file: TestARM.txt
        if neg == False:
            tests.write(chooseBuild().replace("-", "") + "\n")
        else:
            tests.write(chooseBuild() + "\n")
        i += 1


# Scripts
number_of_instructions = raw_input("How many Instructions would you like? :)")
tests.write("@ Here are: " + str(number_of_instructions) + " randomly generated tests @")
printInstruction(num_of_instructions)


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