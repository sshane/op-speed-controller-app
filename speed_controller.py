import sys
live_speed_file = '/data/live_speed_file'

def write_file(a):
    try:
        with open(live_speed_file, 'r') as speed:
            modified_speed=float(speed.read())+a
        with open(live_speed_file, 'w') as speed:
            speed.write(str(modified_speed))
    except: #in case file doesn't exist or is empty
        with open(live_speed_file, 'w') as speed:
            speed.write(str(28.0))

if __name__ == "__main__":
    write_file(int(sys.argv[1]))