## Openpilot Setup Instructions (easy as 1-2)

1. The app should have already uploaded the Python listener file to `/data/openpilot/selfdrive/speed_controller.py`. However if it did not, download the file [here](https://github.com/ShaneSmiskol/op-speed-controller-app/blob/master/speed_controller.py) and place it in that directory via `sftp` using the command:

    `put /path/to/speed_controller.py /data/openpilot/selfdrive/speed_controller.py`.


2. The second and final step is to modify your car's respective `carstate.py` file in `/data/openpilot/selfdrive/car/toyota` (replace `toyota` with your car's make) This can be done with `sftp` as well, using `get` and then `put` with the same syntax above. For `toyota`, find this section of code in `carstate.py`:

    ```python
    self.steer_torque_motor = cp.vl["STEER_TORQUE_SENSOR"]['STEER_TORQUE_EPS']
    # we could use the override bit from dbc, but it's triggered at too high torque values
    self.steer_override = abs(self.steer_torque_driver) > STEER_THRESHOLD

    self.user_brake = 0
    self.v_cruise_pcm = cp.vl["PCM_CRUISE_2"]['SET_SPEED']
    self.pcm_acc_status = cp.vl["PCM_CRUISE"]['CRUISE_STATE']
    self.pcm_acc_active = bool(cp.vl["PCM_CRUISE"]['CRUISE_ACTIVE'])
    self.gas_pressed = not cp.vl["PCM_CRUISE"]['GAS_RELEASED']
    ```
    
    Next delete the line `self.v_cruise_pcm = cp.vl["PCM_CRUISE_2"]['SET_SPEED']`, make room in the gap above, and copy/paste this code there:
    
    ```python
    live_speed_file = '/data/live_speed_file'
    
    if cp.vl["PCM_CRUISE_2"]['SET_SPEED'] != self.speed_limit_prev:
      self.speed_limit_prev = cp.vl["PCM_CRUISE_2"]['SET_SPEED']
      self.v_cruise_pcm = cp.vl["PCM_CRUISE_2"]['SET_SPEED']
      with open(live_speed_file, 'w') as f:
        f.write(str(self.speed_limit_prev))
    else:
      speed = open(live_speed_file, "r")
      self.v_cruise_pcm = float(speed.read())
      ```
      
      Ensure the formatting is correct when you paste it, there should be no extra or missing indents. Then you simply `put` the file back with `sftp` and `reboot` with `ssh`. The full code section should look like this:
      
      ```python
        self.steer_torque_motor = cp.vl["STEER_TORQUE_SENSOR"]['STEER_TORQUE_EPS']
        # we could use the override bit from dbc, but it's triggered at too high torque values
        self.steer_override = abs(self.steer_torque_driver) > STEER_THRESHOLD

        live_speed_file = '/data/live_speed_file'

        if cp.vl["PCM_CRUISE_2"]['SET_SPEED'] != self.speed_limit_prev:
          self.speed_limit_prev = cp.vl["PCM_CRUISE_2"]['SET_SPEED']
          self.v_cruise_pcm = cp.vl["PCM_CRUISE_2"]['SET_SPEED']
          with open(live_speed_file, 'w') as f:
            f.write(str(self.speed_limit_prev))
        else:
          speed = open(live_speed_file, "r")
          self.v_cruise_pcm = float(speed.read())

        self.user_brake = 0
        self.pcm_acc_status = cp.vl["PCM_CRUISE"]['CRUISE_STATE']
        self.pcm_acc_active = bool(cp.vl["PCM_CRUISE"]['CRUISE_ACTIVE'])
        self.gas_pressed = not cp.vl["PCM_CRUISE"]['GAS_RELEASED']
    ```
    
Now you're ready to use the app to control your car's speed! Please contact me on Discord (Shane#6175) or email (shane@smiskol.com) if you are having trouble with this step, or you have a make other than Toyota and I will look into it!
