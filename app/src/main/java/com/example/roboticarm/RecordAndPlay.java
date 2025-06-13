package com.example.roboticarm;

import android.annotation.SuppressLint;
//import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class RecordAndPlay extends AppCompatActivity {
    boolean New_Action_Created = false;
    boolean Previous_Frame_Recorded = true;
    Robot RoboticArm = new Robot();
    int SliderMax = 285;
    int SliderMin = 15;
    List<Frame> action = new ArrayList();
    String action_name = "";
    int current_frame = 0;
    Button gripper;
    String ip;
    LinearLayout linearLayout;
    Button loadAction;
    Button motor1;
    Button motor2;
    Button motor3;
    Button motor4;
    Button motor5;
    SeekBar motorAngle;
    Button neutral;
    Button newAction;
    Button newFrame;
    int p1 = 150;
    int p2 = 150;
    int p3 = 150;
    int p4 = 150;
    int p5 = 150;
    int p6 = 150;
    Button playAction;
    boolean playing = false;
    boolean[] pressed = {false, false, false, false, false, false};
    Button recordFrame;
    int resolution = 1;
    RobotCom robot = new RobotCom();
    Button saveAction;
    ImageButton sb_Dec;
    ImageButton sb_Inc;
    HorizontalScrollView scrollView;
    int selected_button = 1;
    int stepSize = 1;
    TextView tv_motorNum;
    TextView tv_title;
    private Stack<List<Frame>> undoStack = new Stack<>();
    private Stack<List<Frame>> redoStack = new Stack<>();
    private static final int MAX_UNDO_STACK_SIZE = 20;
    private boolean isLooping = false;
    private ImageButton btLoopStop;
    private Button btRecordFrame;
    private boolean isRecording = false;

    /* access modifiers changed from: protected */
    @SuppressLint({"ClickableViewAccessibility"})
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_and_play);

        // Initialize all buttons first
        this.sb_Dec = findViewById(R.id.bt_motor1Dec);
        this.sb_Inc = findViewById(R.id.bt_motor1Inc);
        this.motor1 = findViewById(R.id.bt_motor1);
        this.motor2 = findViewById(R.id.bt_motor2);
        this.motor3 = findViewById(R.id.bt_motor3);
        this.motor4 = findViewById(R.id.bt_motor4);
        this.motor5 = findViewById(R.id.bt_motor5);
        this.gripper = findViewById(R.id.bt_gripper);
        this.neutral = findViewById(R.id.bt_neutral);
        this.recordFrame = findViewById(R.id.bt_recordFrame);
        this.playAction = findViewById(R.id.bt_playAction);
        this.saveAction = findViewById(R.id.bt_saveAction);
        this.loadAction = findViewById(R.id.bt_loadAction);
        this.newAction = findViewById(R.id.bt_newAction);
        this.newFrame = findViewById(R.id.bt_newFrame);
        this.btLoopStop = findViewById(R.id.bt_loop_stop);
        this.btRecordFrame = findViewById(R.id.bt_recordFrame);

        // Initialize other views
        this.motorAngle = findViewById(R.id.sb_motorAngle);
        this.scrollView = findViewById(R.id.sv_frameList);
        this.linearLayout = findViewById(R.id.ll_frames);
        this.tv_title = findViewById(R.id.tv_title);
        this.tv_motorNum = findViewById(R.id.tv_motorNumber);

        // Set up layout
        this.linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Get IP and initialize robot
        this.ip = getIntent().getStringExtra("ip_add");
        this.robot.openTcp(this.ip);
        this.robot.writeDegreesSyncAxMx(this.RoboticArm.IDs, this.RoboticArm.MotorTypes, 
            this.RoboticArm.Neutral_Positions, this.RoboticArm.Neutral_RPMs, 
            (long) this.RoboticArm.Baudrate);

        // Set up seekbar listener
        setupSeekBarListener();

        // Set up button click listeners
        if (motor1 != null) motor1.setOnClickListener(this::onMotor1Click);
        if (motor2 != null) motor2.setOnClickListener(this::onMotor2Click);
        if (motor3 != null) motor3.setOnClickListener(this::onMotor3Click);
        if (motor4 != null) motor4.setOnClickListener(this::onMotor4Click);
        if (motor5 != null) motor5.setOnClickListener(this::onMotor5Click);
        if (gripper != null) gripper.setOnClickListener(this::onGripperClick);
        if (btLoopStop != null) {
            btLoopStop.setImageResource(R.drawable.ic_loop);
        }
    }

    private void setupSeekBarListener() {
        if (motorAngle != null) {
            motorAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                    int adjustedProgress = Math.round((float) (progress / stepSize)) * stepSize;
                    seekBar.setProgress(adjustedProgress);
                    
                    switch (selected_button) {
                        case 1:
                            p1 = adjustedProgress;
                            break;
                        case 2:
                            p2 = adjustedProgress;
                            break;
                        case 3:
                            p3 = adjustedProgress;
                            break;
                        case 4:
                            p4 = adjustedProgress;
                            break;
                        case 5:
                            p5 = adjustedProgress;
                            break;
                        case 6:
                            p6 = adjustedProgress;
                            break;
                    }
                    
                    // Send updated positions to robot
                    robot.writeDegreesSyncAxMx(
                        new byte[]{1, 2, 3, 4, 5, 6},
                        new byte[]{0, 0, 0, 0, 0, 0},
                        new double[]{(double) p1, (double) p2, (double) p3, (double) p4, (double) p5, (double) p6},
                        new double[]{20.0d, 20.0d, 20.0d, 20.0d, 20.0d, 20.0d},
                        57142
                    );
                }

                public void onStartTrackingTouch(SeekBar seekBar) {}

                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private int getSelectedMotorPosition() {
        switch (selected_button) {
            case 1: return p1;
            case 2: return p2;
            case 3: return p3;
            case 4: return p4;
            case 5: return p5;
            case 6: return p6;
            default: return 150;
        }
    }

    // Separate click methods
    public void onMotor1Click(View view) {
        this.selected_button = 1;
        if (this.pressed[0]) {
            this.motorAngle.setProgress(this.p1);
        } else {
            this.motorAngle.setProgress(150);
        }
        this.pressed[0] = true;
        this.tv_motorNum.setText(R.string.motor1);
        Toast.makeText(this, "Motor 1 clicked", Toast.LENGTH_SHORT).show();
    }

    public void onMotor2Click(View view) {
        Toast.makeText(this, "Motor 2 clicked", Toast.LENGTH_SHORT).show();
        this.selected_button = 2;
        if (this.pressed[1]) {
            this.motorAngle.setProgress(this.p2);
        } else {
            this.motorAngle.setProgress(150);
        }
        this.pressed[1] = true;
        this.tv_motorNum.setText(R.string.motor2);
    }

    public void onMotor3Click(View view) {
        this.selected_button = 3;
        if (this.pressed[2]) {
            this.motorAngle.setProgress(this.p3);
        } else {
            this.motorAngle.setProgress(150);
        }
        this.pressed[2] = true;
        this.tv_motorNum.setText(R.string.motor3);
        Toast.makeText(this, "Motor 3 clicked", Toast.LENGTH_SHORT).show();
    }

    public void onMotor4Click(View view) {
        this.selected_button = 4;
        if (this.pressed[3]) {
            this.motorAngle.setProgress(this.p4);
        } else {
            this.motorAngle.setProgress(150);
        }
        this.pressed[3] = true;
        this.tv_motorNum.setText(R.string.motor4);
        Toast.makeText(this, "Motor 4 clicked", Toast.LENGTH_SHORT).show();
    }

    public void onMotor5Click(View view) {
        this.selected_button = 5;
        if (this.pressed[4]) {
            this.motorAngle.setProgress(this.p5);
        } else {
            this.motorAngle.setProgress(150);
        }
        this.pressed[4] = true;
        this.tv_motorNum.setText(R.string.motor5);
        Toast.makeText(this, "Motor 5 clicked", Toast.LENGTH_SHORT).show();
    }

    public void onGripperClick(View view) {

        Toast.makeText(this, "Gripper clicked", Toast.LENGTH_SHORT).show();
        this.selected_button = 6;
        if (this.pressed[5]) {
            this.motorAngle.setProgress(this.p6);
        } else {
            this.motorAngle.setProgress(150);
        }
        this.pressed[5] = true;
        this.tv_motorNum.setText(R.string.gripper);
    }

    public void inc_slider(View view) {
        switch (this.selected_button) {
            case 1:
                if (this.motorAngle.getProgress() < this.SliderMax) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() + this.resolution);
                    return;
                }
                return;
            case 2:
                if (this.motorAngle.getProgress() < this.SliderMax) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() + this.resolution);
                    return;
                }
                return;
            case 3:
                if (this.motorAngle.getProgress() < this.SliderMax) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() + this.resolution);
                    return;
                }
                return;
            case 4:
                if (this.motorAngle.getProgress() < this.SliderMax) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() + this.resolution);
                    return;
                }
                return;
            case 5:
                if (this.motorAngle.getProgress() < this.SliderMax) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() + this.resolution);
                    return;
                }
                return;
            case 6:
                if (this.motorAngle.getProgress() < this.SliderMax) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() + this.resolution);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void dec_slider(View view) {
        switch (this.selected_button) {
            case 1:
                if (this.motorAngle.getProgress() > this.SliderMin) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() - this.resolution);
                    return;
                }
                return;
            case 2:
                if (this.motorAngle.getProgress() > this.SliderMin) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() - this.resolution);
                    return;
                }
                return;
            case 3:
                if (this.motorAngle.getProgress() > this.SliderMin) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() - this.resolution);
                    return;
                }
                return;
            case 4:
                if (this.motorAngle.getProgress() > this.SliderMin) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() - this.resolution);
                    return;
                }
                return;
            case 5:
                if (this.motorAngle.getProgress() > this.SliderMin) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() - this.resolution);
                    return;
                }
                return;
            case 6:
                if (this.motorAngle.getProgress() > this.SliderMin) {
                    this.motorAngle.setProgress(this.motorAngle.getProgress() - this.resolution);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void motor_on_click(View view) {
        int id = view.getId(); // Get the clicked button ID

        switch (id) {
            case 2131230761:
                this.selected_button = 6;
                if (this.pressed[5]) {
                    this.motorAngle.setProgress(this.p6);
                } else {
                    this.motorAngle.setProgress(150);
                }
                this.pressed[5] = true;
                this.tv_motorNum.setText(R.string.gripper);
                return;
            case 2131230760:
                this.selected_button = 1;
                if (this.pressed[0]) {
                    this.motorAngle.setProgress(this.p1);
                } else {
                    this.motorAngle.setProgress(150);
                }
                this.pressed[0] = true;
                this.tv_motorNum.setText(R.string.motor1);
                return;
            case 2131230764:
                this.selected_button = 2;
                if (this.pressed[1]) {
                    this.motorAngle.setProgress(this.p2);
                } else {
                    this.motorAngle.setProgress(150);
                }
                this.pressed[1] = true;
                this.tv_motorNum.setText(R.string.motor2);
                return;
            case 2131230768:
                this.selected_button = 3;
                if (this.pressed[2]) {
                    this.motorAngle.setProgress(this.p3);
                } else {
                    this.motorAngle.setProgress(150);
                }
                this.pressed[2] = true;
                this.tv_motorNum.setText(R.string.motor3);
                return;
            case 2131230772:
                this.selected_button = 4;
                if (this.pressed[3]) {
                    this.motorAngle.setProgress(this.p4);
                } else {
                    this.motorAngle.setProgress(150);
                }
                this.pressed[3] = true;
                this.tv_motorNum.setText(R.string.motor4);
                return;
            case 2131230776:
                this.selected_button = 5;
                if (this.pressed[4]) {
                    this.motorAngle.setProgress(this.p5);
                } else {
                    this.motorAngle.setProgress(150);
                }
                this.pressed[4] = true;
                this.tv_motorNum.setText(R.string.motor5);
                return;
            default:
                return;
        }
    }

    public void neutral_on_click(View view) {
        this.p1 = 150;
        this.p2 = 150;
        this.p3 = 150;
        this.p4 = 150;
        this.p5 = 150;
        this.p6 = 150;
        this.motorAngle.setProgress(150);
        this.robot.writeDegreesSyncAxMx(this.RoboticArm.IDs, this.RoboticArm.MotorTypes, this.RoboticArm.Neutral_Positions, this.RoboticArm.Neutral_RPMs, (long) this.RoboticArm.Baudrate);
    }

    public void newAction_on_click(View view) {
        if (!this.New_Action_Created) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle((CharSequence) "Action Name");
            final EditText input = new EditText(this);
            input.setInputType(1);
            builder.setView((View) input);
            builder.setPositiveButton((CharSequence) "Create", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    RecordAndPlay.this.action_name = input.getText().toString();
                    TextView textView = RecordAndPlay.this.tv_title;
                    textView.setText("title:" + RecordAndPlay.this.action_name);
                    RecordAndPlay.this.action.clear();
                    RecordAndPlay.this.action.add(new Frame(RecordAndPlay.this.RoboticArm.Neutral_Positions));
                    RecordAndPlay.this.action.get(0).duration = 1.0d;
                    RecordAndPlay.this.action.get(0).pause = 1.0d;
                    RecordAndPlay.this.add_neutral_frame_to_scroll_view();
                    RecordAndPlay.this.New_Action_Created = true;
                    Toast.makeText(RecordAndPlay.this, "New Action created", Toast.LENGTH_SHORT).show();
                }
            });
            builder.show();
            return;
        }
        Toast.makeText(this, "New Action already created", Toast.LENGTH_SHORT).show();
    }

    public void newFrame_on_click(View view) {
        if (!this.New_Action_Created) {
            Toast.makeText(this, "Create a new Action!", Toast.LENGTH_SHORT).show();
        } else if (this.Previous_Frame_Recorded) {
            this.p1 = 150;
            this.p2 = 150;
            this.p3 = 150;
            this.p4 = 150;
            this.p5 = 150;
            this.p6 = 150;
            this.motorAngle.setProgress(150);
            this.current_frame++;
            Button button = new Button(this);
            button.setText("N");
            button.setId(this.current_frame);
            button.setBackground(getDrawable(R.drawable.framelist_button_background));
            button.setTextColor(getColor(R.color.buttonTextColor));
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    RecordAndPlay.this.edit_frame(view);
                }
            });
            this.linearLayout.addView(button);
            this.scrollView.scrollTo(this.linearLayout.getRight(), 0);
            this.Previous_Frame_Recorded = false;
        } else {
            Toast.makeText(this, "Record the frame!", Toast.LENGTH_SHORT).show();
        }
        this.action.add(new Frame(new double[]{(double) this.p1, (double) this.p2, (double) this.p3, (double) this.p4, (double) this.p5, (double) this.p6}));
        this.action.get(this.current_frame).duration = 1.0d;
        this.action.get(this.current_frame).pause = 1.0d;
        this.Previous_Frame_Recorded = true;
    }

    public void add_neutral_frame_to_scroll_view() {
        Button button = new Button(this);
        button.setBackground(getDrawable(R.drawable.framelist_button_background));
        button.setTextColor(getColor(R.color.buttonTextColor));
        button.setText("N");
        button.setId(this.current_frame);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                RecordAndPlay.this.edit_frame(view);
            }
        });
        this.linearLayout.addView(button);
        Toast.makeText(this, "Neutral frame added", Toast.LENGTH_SHORT).show();
    }

    public void edit_frame(View view) {
        try {
            final int selected_frame = view.getId();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle((CharSequence) "Frame");
            LinearLayout layout = new LinearLayout(this);
            LinearLayout linearLayout2 = this.linearLayout;
            layout.setOrientation(LinearLayout.VERTICAL);
            TextView tv_duration = new TextView(this);
            tv_duration.setText("duration");
            final EditText et_duration = new EditText(this);
            et_duration.setText(String.valueOf(this.action.get(selected_frame).duration));
            TextView tv_delay = new TextView(this);
            tv_delay.setText("delay");
            final EditText et_delay = new EditText(this);
            et_delay.setText(String.valueOf(this.action.get(selected_frame).pause));
            et_duration.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            et_delay.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            layout.addView(tv_duration);
            layout.addView(et_duration);
            layout.addView(tv_delay);
            layout.addView(et_delay);
            builder.setView((View) layout);
            builder.setPositiveButton((CharSequence) "Save", (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    RecordAndPlay.this.action.get(selected_frame).duration = Double.valueOf(et_duration.getText().toString()).doubleValue();
                    RecordAndPlay.this.action.get(selected_frame).pause = Double.valueOf(et_delay.getText().toString()).doubleValue();
                }
            });
            
            // Add Delete button if it's not the neutral frame (frame 0)
            if (selected_frame != 0) {
                builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFrame(selected_frame);
                    }
                });
            }
            
            builder.show();
        } catch (Exception e) {
            Toast.makeText(this, "Record frame first", Toast.LENGTH_SHORT).show();
        }
    }

    public void loadAction_on_click(View view) {
        List<File> files = getListFiles(getFilesDir());
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Action Name");
        final Spinner input = new Spinner(this);
        String[] action_names = new String[files.size()];
        for (int i = 0; i < files.size(); i++) {
            action_names[i] = files.get(i).getName();
        }
        ArrayAdapter dataAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, action_names);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        input.setAdapter(dataAdapter);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try {
                    RecordAndPlay.this.action_name = input.getSelectedItem().toString().replace(".txt", "");
                    TextView textView = RecordAndPlay.this.tv_title;
                    textView.setText("title:" + RecordAndPlay.this.action_name);
                    RecordAndPlay.this.action.clear();
                    String readFile = RecordAndPlay.this.readFromFile(RecordAndPlay.this, RecordAndPlay.this.action_name.concat(".txt"));
                    RecordAndPlay.this.action = RecordAndPlay.this.get_action_from_string(readFile);
                    RecordAndPlay.this.update_scrollView_frames();
                    Toast.makeText(RecordAndPlay.this, "Action loaded", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(RecordAndPlay.this, "No Actions", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.show();
    }

    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<>();
        for (File file : parentDir.listFiles()) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else if (file.getName().endsWith(".txt")) {
                inFiles.add(file);
            }
        }
        return inFiles;
    }

    public void update_scrollView_frames() {
        if (this.linearLayout.getChildCount() > 0) {
            this.linearLayout.removeAllViews();
        }
        
        // Update current frame count
        this.current_frame = this.action.size() - 1;
        
        // Add all frames to the scroll view
        for (int i = 0; i < this.action.size(); i++) {
            Button frameButton = new Button(this);
            
            // Set button text based on frame type
            if (i == 0) {
                frameButton.setText("N"); // Neutral frame
            } else {
                frameButton.setText("frame " + i); // Regular frame
            }
            
            frameButton.setId(i);
            frameButton.setBackground(getDrawable(R.drawable.framelist_button_background));
            frameButton.setTextColor(getColor(R.color.buttonTextColor));
            frameButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    RecordAndPlay.this.edit_frame(view);
                }
            });
            
            this.linearLayout.addView(frameButton);
        }
        
        this.New_Action_Created = true;
    }

    public List<Frame> get_action_from_string(String str) {
        String[] lines = str.split("E");
        List<Frame> act = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            double dur = Double.valueOf(lines[i].split("D")[0]).doubleValue();
            lines[i] = lines[i].split("D")[1];
            double pau = Double.valueOf(lines[i].split("P")[0]).doubleValue();
            lines[i] = lines[i].split("P")[1];
            String[] motorvals = lines[i].split("A");
            double[] positions = new double[motorvals.length];
            for (int j = 0; j < motorvals.length; j++) {
                positions[j] = Double.valueOf(motorvals[j]).doubleValue();
            }
            Frame f = new Frame(positions);
            f.duration = dur;
            f.pause = pau;
            act.add(f);
        }
        return act;
    }

    /* access modifiers changed from: private */
    public String readFromFile(Context context, String action_name2) {
        try {
            InputStream inputStream = context.openFileInput(action_name2);
            if (inputStream == null) {
                return "";
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            while (true) {
                String readLine = bufferedReader.readLine();
                String receiveString = readLine;
                if (readLine != null) {
                    stringBuilder.append(receiveString);
                } else {
                    inputStream.close();
                    return stringBuilder.toString();
                }
            }
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
            return "";
        } catch (IOException e2) {
            Log.e("login activity", "Can not read file: " + e2.toString());
            return "";
        }
    }

    public void recordFrame_on_click(View view) {
        try {
            if (!New_Action_Created) {
                Toast.makeText(this, "Create a new Action!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save state before recording new frame
            saveStateForUndo();
            current_frame++;
            
            // Create and add frame button
            Button frameButton = new Button(this);
            frameButton.setText("frame " + Integer.toString(current_frame));
            frameButton.setId(current_frame);
            frameButton.setBackground(getDrawable(R.drawable.framelist_button_background));
            frameButton.setTextColor(getColor(R.color.buttonTextColor));
            frameButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    edit_frame(view);
                }
            });
            
            linearLayout.addView(frameButton);
            scrollView.scrollTo(linearLayout.getRight(), 0);
            Previous_Frame_Recorded = false;
            
            // Add frame to action list
            action.add(new Frame(new double[]{(double) p1, (double) p2, (double) p3, (double) p4, (double) p5, (double) p6}));
            action.get(current_frame).duration = 1.0d;
            action.get(current_frame).pause = 1.0d;
            Previous_Frame_Recorded = true;
            
            Toast.makeText(this, "Frame recorded", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error recording frame: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void playAction_on_click(View view) {
        Toast.makeText(this, "Playing Action", Toast.LENGTH_SHORT).show();
        if (!this.playing) {
            this.playing = true;
            new PlayActionTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
        }
    }

    public void saveAction_on_click(View view) {
        if (this.action_name.compareTo("") != 0) {
            String data = "";
            for (int i = 0; i < this.action.size(); i++) {
                String data2 = data.concat(Double.toString(this.action.get(i).duration)).concat("D").concat(Double.toString(this.action.get(i).pause)).concat("P");
                for (int j = 0; j < this.action.get(i).positions.length; j++) {
                    data2 = data2.concat(Double.toString(this.action.get(i).positions[j]));
                    if (j < this.action.get(i).positions.length - 1) {
                        data2 = data2.concat("A");
                    }
                }
                data = data2.concat("E");
            }
            writeToFile(data, this);
            Toast.makeText(this, "Action Saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeToFile(String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(this.action_name.replace(".txt", "").concat(".txt"), 0));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private class PlayActionTask extends AsyncTask<Void, Void, Void> {
        private PlayActionTask() {
        }

        /* access modifiers changed from: protected */
        public Void doInBackground(Void... voids) {
            int i;
            double[] rpms = new double[RecordAndPlay.this.RoboticArm.NumberOfMotors];
            boolean not_nuetral = false;
            int j = 0;
            while (true) {
                i = 1;
                if (j >= RecordAndPlay.this.RoboticArm.NumberOfMotors) {
                    break;
                }
                if (RecordAndPlay.this.action.get(RecordAndPlay.this.action.size() - 1).positions[j] != 150.0d) {
                    not_nuetral = true;
                }
                j++;
            }
            if (not_nuetral) {
                double[] positions = RecordAndPlay.this.action.get(RecordAndPlay.this.action.size() - 1).positions;
                for (int j2 = 0; j2 < RecordAndPlay.this.RoboticArm.NumberOfMotors; j2++) {
                    double diff = Math.abs(RecordAndPlay.this.RoboticArm.Neutral_Positions[j2] - positions[j2]);
                    if (RecordAndPlay.this.RoboticArm.MotorTypes[j2] == RecordAndPlay.this.RoboticArm.AX12 || RecordAndPlay.this.RoboticArm.MotorTypes[j2] == RecordAndPlay.this.RoboticArm.AX18) {
                        rpms[j2] = (diff / 180.0d) / 0.03333333333333333d;
                    } else {
                        rpms[j2] = (diff / 360.0d) / 0.03333333333333333d;
                    }
                }
                boolean z = not_nuetral;
                double[] dArr = positions;
                RecordAndPlay.this.robot.writeDegreesSyncAxMx(RecordAndPlay.this.RoboticArm.IDs, RecordAndPlay.this.RoboticArm.MotorTypes, RecordAndPlay.this.RoboticArm.Neutral_Positions, rpms, (long) RecordAndPlay.this.RoboticArm.Baudrate);
                try {
                    Thread.sleep((long) ((int) ((0.03333333333333333d + 0.03333333333333333d) * 60.0d * 1000.0d)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (RecordAndPlay.this.action.size() > 1) {
                while (true) {
                    int i2 = i;
                    if (i2 >= RecordAndPlay.this.action.size()) {
                        break;
                    }
                    double dur = RecordAndPlay.this.action.get(i2).duration / 60.0d;
                    double pau = RecordAndPlay.this.action.get(i2).pause / 60.0d;
                    for (int j3 = 0; j3 < RecordAndPlay.this.RoboticArm.NumberOfMotors; j3++) {
                        double diff2 = Math.abs(RecordAndPlay.this.action.get(i2).positions[j3] - RecordAndPlay.this.action.get(i2 - 1).positions[j3]);
                        if (RecordAndPlay.this.RoboticArm.MotorTypes[j3] == RecordAndPlay.this.RoboticArm.AX12 || RecordAndPlay.this.RoboticArm.MotorTypes[j3] == RecordAndPlay.this.RoboticArm.AX18) {
                            rpms[j3] = (diff2 / 180.0d) / dur;
                        } else {
                            rpms[j3] = (diff2 / 360.0d) / dur;
                        }
                    }
                    RecordAndPlay.this.robot.writeDegreesSyncAxMx(RecordAndPlay.this.RoboticArm.IDs, RecordAndPlay.this.RoboticArm.MotorTypes, RecordAndPlay.this.action.get(i2).positions, rpms, (long) RecordAndPlay.this.RoboticArm.Baudrate);
                    try {
                        Thread.sleep((long) ((int) ((dur + pau) * 60.0d * 1000.0d)));
                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                    i = i2 + 1;
                }
            }
            RecordAndPlay.this.playing = false;
            return null;
        }
    }

    public void delete_on_click(View view) {
        if (this.linearLayout.getChildCount() > 0) {
            saveStateForUndo(); // Save state before clearing frames
            this.linearLayout.removeAllViews();
        }
        this.action.clear();
        this.current_frame = 0;
        this.tv_title.setText("title:");
        this.New_Action_Created = false;
        Toast.makeText(this, "Frame list cleared", Toast.LENGTH_SHORT).show();
    }

    public void deleteFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < action.size()) {
            saveStateForUndo(); // Save state before deleting frame
            
            // Remove the frame from action list
            action.remove(frameIndex);
            
            // Update current frame count
            current_frame = action.size() - 1;
            
            // Update the UI
            update_scrollView_frames();
            
            Toast.makeText(this, "Frame deleted", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveStateForUndo() {
        List<Frame> currentState = new ArrayList<>();
        for (Frame frame : action) {
            currentState.add(new Frame(frame.positions));
            currentState.get(currentState.size() - 1).duration = frame.duration;
            currentState.get(currentState.size() - 1).pause = frame.pause;
        }
        undoStack.push(currentState);
        if (undoStack.size() > MAX_UNDO_STACK_SIZE) {
            undoStack.remove(0);
        }
        redoStack.clear(); // Clear redo stack when new action is performed
    }

    public void undo_on_click(View view) {
        if (!undoStack.isEmpty()) {
            // Save current state to redo stack
            List<Frame> currentState = new ArrayList<>();
            for (Frame frame : action) {
                currentState.add(new Frame(frame.positions));
                currentState.get(currentState.size() - 1).duration = frame.duration;
                currentState.get(currentState.size() - 1).pause = frame.pause;
            }
            redoStack.push(currentState);
            
            // Restore previous state
            action = undoStack.pop();
            current_frame = action.size() - 1;
            
            // Update UI with correct frame labels
            update_scrollView_frames();
            
            Toast.makeText(this, "Undo successful", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
        }
    }

    public void redo_on_click(View view) {
        if (!redoStack.isEmpty()) {
            // Save current state to undo stack
            List<Frame> currentState = new ArrayList<>();
            for (Frame frame : action) {
                currentState.add(new Frame(frame.positions));
                currentState.get(currentState.size() - 1).duration = frame.duration;
                currentState.get(currentState.size() - 1).pause = frame.pause;
            }
            undoStack.push(currentState);
            
            // Restore next state
            action = redoStack.pop();
            current_frame = action.size() - 1;
            
            // Update UI with correct frame labels
            update_scrollView_frames();
            
            Toast.makeText(this, "Redo successful", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
        }
    }

    public void loop_stop_on_click(View view) {
        isLooping = !isLooping;
        if (isLooping) {
            btLoopStop.setImageResource(R.drawable.ic_stop);
            startLooping();
        } else {
            btLoopStop.setImageResource(R.drawable.ic_loop);
            stopLooping();
        }
    }

    private void startLooping() {
        if (action.isEmpty()) {
            Toast.makeText(this, "No frames to loop", Toast.LENGTH_SHORT).show();
            isLooping = false;
            btLoopStop.setImageResource(R.drawable.ic_loop);
            return;
        }

        new Thread(() -> {
            while (isLooping) {
                for (int i = 0; i < action.size(); i++) {
                    if (!isLooping) break;
                    
                    Frame frame = action.get(i);
                    double[] rpms = new double[RoboticArm.NumberOfMotors];
                    
                    // Calculate RPMs for each motor
                    for (int j = 0; j < RoboticArm.NumberOfMotors; j++) {
                        double diff = Math.abs(frame.positions[j] - (i > 0 ? action.get(i-1).positions[j] : RoboticArm.Neutral_Positions[j]));
                        if (RoboticArm.MotorTypes[j] == RoboticArm.AX12 || RoboticArm.MotorTypes[j] == RoboticArm.AX18) {
                            rpms[j] = (diff / 180.0d) / (frame.duration / 60.0d);
                        } else {
                            rpms[j] = (diff / 360.0d) / (frame.duration / 60.0d);
                        }
                    }

                    // Send frame to robot
                    robot.writeDegreesSyncAxMx(RoboticArm.IDs, RoboticArm.MotorTypes, frame.positions, rpms, (long) RoboticArm.Baudrate);
                    
                    try {
                        // Wait for duration + pause
                        Thread.sleep((long) ((frame.duration + frame.pause) * 1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }

    private void stopLooping() {
        isLooping = false;
    }
}
