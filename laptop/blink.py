leftMotor = None
rightMotor = None
facing = 0


def setup():
  for i in range(12):
    pinMode(i,OUTPUT)
  global leftMotor,rightMotor
  leftMotor = Motor([8])
  rightMotor = Motor([9])
  serialWrite(0,1,16)
  serialWrite(2,3,17)
  
def loop():
  shouldMove = False
  if(channelActive(0)):
    turnToFace(0)
    shouldMove = True
  elif(channelActive(1)):
    turnToFace(2)
    shouldMove = True
  elif(channelActive(2)):
    turnToFace(1)
    shouldMove = True
  elif(channelActive(3)):
    turnToFace(3)
    shouldMove = True
  if shouldMove:
    leftMotor.activate()
    rightMotor.activate()
    tick()
 
class Motor:
    def __init__(self, pins):
        self.pins = pins
    def activate(self):
        for i in self.pins:
            digitalWrite(i,HIGH)
    def deactivate(self):
        for i in self.pins:
            digitalWrite(i,LOW)

def serialWrite(clockPin,dataPin,val):
  for bit in range (8):
    if (val & (1<<bit)) == (1<<bit):
      digitalWrite(dataPin,HIGH)
      digitalWrite(clockPin,HIGH)
      digitalWrite(clockPin,LOW)
    else:
      digitalWrite(dataPin,LOW)
      digitalWrite(clockPin,HIGH)
      digitalWrite(clockPin,LOW)

def turnToFace(direction):
    while facing != direction:
        turnLeft()

def turnLeft():
    rightMotor.activate()
    leftMotor.deactivate()
    global facing
    facing += 1
    facing %= 4
    tick()
        
def channelActive(channel):
    selectChannel(channel)
    val = digitalRead(12)
    return val == 1024

def selectChannel(channel):
  for bit in range (2):
    if (channel & (1<<bit)) == (1<<bit):
      digitalWrite(bit+4,HIGH)
    else:
      digitalWrite(bit+4,LOW)
