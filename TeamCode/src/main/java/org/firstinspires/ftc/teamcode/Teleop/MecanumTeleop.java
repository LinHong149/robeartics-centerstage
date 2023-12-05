package org.firstinspires.ftc.teamcode.Teleop;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;


@TeleOp(name="Mecanum Teleop", group="Mecanum")
public class MecanumTeleop extends LinearOpMode {

    // Declare servos and motors
    public Servo claw, wrist, lArm, rArm;
    public DcMotor fr, fl, br, bl, lSlide, rSlide;

    boolean clawOpen = false;
    boolean isOuttaking = false;

    double lIntake = 0.77;
    double rIntake = 0.29;
    double lOuttake = 0.16;
    double rOuttake = 0.90;

    double P = 0.1; // Tune these values
    double I = 0;
    double D = 0;
    double integral = 0;
    double previous_error = 0;
    double setpoint = 0;

    FtcDashboard dashboard;
    Telemetry dashboardTelemetry;

    // Initialize standard Hardware interfaces
    public void initHardware() {
        // Servos
        claw = hardwareMap.get(Servo.class, "claw");
        wrist = hardwareMap.get(Servo.class, "wrist");
        lArm = hardwareMap.get(Servo.class, "lArm");
        rArm = hardwareMap.get(Servo.class, "rArm");

        wrist.setPosition(0.73);
        claw.setPosition(0.55); // Closed
        // lArm.setPosition(0.77); // Intaking
        lArm.setPosition(0.77); // Intaking
        rArm.setPosition(0.29); // Intaking

        // Motors
        fr = hardwareMap.get(DcMotor.class, "fr");
        fl = hardwareMap.get(DcMotor.class, "fl");
        br = hardwareMap.get(DcMotor.class, "br");
        bl = hardwareMap.get(DcMotor.class, "bl");
        lSlide = hardwareMap.get(DcMotor.class, "lSlide");
        rSlide = hardwareMap.get(DcMotor.class, "rSlide");

        fr.setPower(0);
        fl.setPower(0);
        br.setPower(0);
        bl.setPower(0);
        lSlide.setPower(0);
        rSlide.setPower(0);

        fr.setDirection(DcMotor.Direction.REVERSE);
        br.setDirection(DcMotor.Direction.REVERSE);
        fl.setDirection(DcMotor.Direction.FORWARD);
        bl.setDirection(DcMotor.Direction.FORWARD);
        lSlide.setDirection(DcMotor.Direction.REVERSE);
        rSlide.setDirection(DcMotor.Direction.FORWARD);

        fr.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        fl.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        br.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        bl.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        lSlide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rSlide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        fr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        fl.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        br.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        bl.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    @Override
    public void runOpMode() {
        initHardware();
        telemetry.addData("Status", "Initialized");
        telemetry.update();

        dashboard = FtcDashboard.getInstance();
        dashboardTelemetry = dashboard.getTelemetry();

        waitForStart();

        while (opModeIsActive()) {
            telemetry.addData("Status", "Running");

            // Motors: driving
            double drive = -gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double rotate = gamepad1.right_stick_x;

            double denominator = Math.max(Math.abs(drive) + Math.abs(strafe) + Math.abs(rotate), 1);

            double frontLeftPower = (drive + strafe + rotate) / denominator * 1.4;
            double backLeftPower = (drive - strafe + rotate) / denominator * 1.4;
            double frontRightPower = (drive - strafe - rotate) / denominator * 1.4;
            double backRightPower = (drive + strafe - rotate) / denominator * 1.4;

            fr.setPower(frontRightPower);
            fl.setPower(frontLeftPower);
            br.setPower(backRightPower);
            bl.setPower(backLeftPower);

            telemetry.addData("strafe","%.2f", strafe);
            telemetry.addData("drive","%.2f", drive);
            telemetry.addData("rotate","%.2f", rotate);

            telemetry.addData("lSlide Pos", lSlide.getCurrentPosition());
            telemetry.addData("rSlide Pos", rSlide.getCurrentPosition());

            // Slides
            if (gamepad1.right_trigger > 0) {
                setpoint += 1; // Increment the setpoint
            } else if (gamepad1.left_trigger > 0) {
                setpoint -= 1; // Decrement the setpoint
            }

            // PID Control for Slides
            double error = setpoint - lSlide.getCurrentPosition();
            integral += (error * 0.02); // Assuming loop time of 20 milliseconds
            double derivative = (error - previous_error) / 0.02;
            double output = P * error + I * integral + D * derivative;
            lSlide.setPower(output);
            rSlide.setPower(output);
            previous_error = error;

            // Send data to FTC Dashboard
            TelemetryPacket packet = new TelemetryPacket();
            packet.put("Setpoint", setpoint);
            packet.put("Current Position", lSlide.getCurrentPosition());
            packet.put("Error", error);
            dashboard.sendTelemetryPacket(packet);

            if (gamepad1.right_trigger > 0 && lSlide.getCurrentPosition() < 455) { // Raise lift
                telemetry.addData("dpad_up","active");
                telemetry.update();
                lSlide.setPower(0.4);
                rSlide.setPower(0.4);
            } else if (gamepad1.left_trigger > 0) { // Lower lift
                lSlide.setPower(-0.25);
                rSlide.setPower(-0.25);
            } else { // Stops slides
                lSlide.setPower(0);
                rSlide.setPower(0);
            }


            // Open close
            if (gamepad1.a || gamepad1.b) {
                telemetry.addData("Claw","active");
                telemetry.update();
                if (clawOpen) {
                    clawOpen = false;
                    claw.setPosition(0.55); // Close the claw
                } else {
                    clawOpen = true;
                    claw.setPosition(0.2); // Open the claw
                }
                sleep(200);
            }

            // Intake and outtake
            if (gamepad1.x) {
                if (isOuttaking) {
                    telemetry.addData("Extention","intake");
                    telemetry.update();
                    isOuttaking = false;
                    wrist.setPosition(0.72); // Retract
                    lArm.setPosition(lIntake); // Parallel
                    rArm.setPosition(rIntake);
                } else {
                    telemetry.addData("Extention","outtake");
                    telemetry.update();
                    isOuttaking = true;
                    wrist.setPosition(0.0); //Extend
                    lArm.setPosition(lOuttake);
                    rArm.setPosition(rOuttake);
                }
                sleep(200);
            }
            telemetry.update();
        }
    }
}