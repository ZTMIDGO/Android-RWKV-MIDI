package com.litesnap.open.rwkv.midi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Mido {
    public static long second2tick(double second, int ticks_per_beat, int tempo){
        double scale = tempo * 1e-6 / ticks_per_beat;
        return Math.round(second / scale);
    }

    public static float wait_token_to_delta(VocabConfig config, int value){
        return config.max_wait_time * 1f / config.wait_events * value;
    }

    public static int[] note_token_to_data(VocabConfig config, String token){
        String[] array = token.trim().split(":");
        String instr_str = array[0];
        String note_str = array[1];
        String velocity_str = array[2];

        int instr_bin = config._short_instrument_names_str_to_int.get(instr_str);
        int note = Integer.parseInt(note_str, 16);
        int velocity = bin_to_velocity(config, Integer.parseInt(velocity_str, 16));
        return new int[]{instr_bin, note, velocity};
    }

    public static int bin_to_velocity(VocabConfig config, int bin){
        float binsize = config.velocity_events * 1f / (config.velocity_bins - 1);

        if (config.velocity_exp == 1){
            return (int) Math.max(0, Math.ceil(bin * binsize - 1));
        }else {
            return (int) Math.max(0, Math.ceil(config.velocity_events * (Math.log(((config.velocity_exp - 1) * binsize * bin) / config.velocity_events + 1) / Math.log(config.velocity_exp)) - 1));
        }
    }

    public static byte[] createHeader(int type, int trackLen, int ticks_per_beat){
        byte[] mthd = "MThd".getBytes();
        byte[] t = HexUtils.hexToBytes(Integer.toHexString(type), 2);
        byte[] tl = HexUtils.hexToBytes(Integer.toHexString(trackLen), 2);
        byte[] tpb = HexUtils.hexToBytes(Integer.toHexString(ticks_per_beat), 2);
        byte[] len = HexUtils.hexToBytes(Integer.toHexString(t.length + tl.length + tpb.length), 4);
        byte[] array = new byte[mthd.length + len.length + t.length + tl.length + tpb.length];
        int index = 0;
        for (int i = 0; i < mthd.length; i++) array[index ++] = mthd[i];
        for (int i = 0; i < len.length; i++) array[index ++] = len[i];
        for (int i = 0; i < t.length; i++) array[index ++] = t[i];
        for (int i = 0; i < tl.length; i++) array[index ++] = tl[i];
        for (int i = 0; i < tpb.length; i++) array[index ++] = tpb[i];
        return array;
    }

    public static byte[] createTrack(List<MidiMessage> messages) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{0, (byte) 255, 1, 15, 71, 101, 110, 101, 114, 97, 116, 101, 100, 32, 98, 121, 32, 65, 105});
        out.write(new byte[]{0, (byte) 255, 81, 3, 7, (byte) 161, 32});

        Byte running_status_byte = null;
        for (MidiMessage msg : messages){
            System.out.println(msg.time+"  "+Arrays.toString(encode_variable_int(msg.time)));
            out.write(encode_variable_int(msg.time));
            if (msg.is_meta){

            }else {
                byte[] msg_bytes = encodeMessage(msg);
                byte status_byte = msg_bytes[0];
                if (running_status_byte != null && status_byte == running_status_byte){
                    out.write(Arrays.copyOfRange(msg_bytes, 1, msg_bytes.length));
                }else {
                    out.write(msg_bytes);
                }

                if (status_byte < 0xf0){
                    running_status_byte = status_byte;
                }else {
                    running_status_byte = null;
                }
            }
        }
        out.write(new byte[]{0, (byte) 255, 47, 0});
        byte[] bytes = out.toByteArray();
        out.close();

        out = new ByteArrayOutputStream();
        out.write("MTrk".getBytes());
        out.write(HexUtils.hexToBytes(Integer.toHexString(bytes.length), 4));
        out.write(bytes);
        bytes = out.toByteArray();
        out.close();
        return bytes;
    }

    public static byte[] encode_variable_int(int val){
        List<Integer> list = new ArrayList<>();
        while (val > 0){
            list.add(val & 0x7f);
            val >>= 7;
        }

        if (!list.isEmpty()){
            Collections.reverse(list);
            Integer[] integers = list.toArray(new Integer[list.size()]);
            for (int i = 0; i < list.size() - 1; i++){
                integers[i] = list.get(i) | 0x80;
            }

            byte[] bytes = new byte[integers.length];
            for (int i = 0; i < integers.length; i++){
                int value = integers[i];
                bytes[i] = (byte) value;
            }
            return bytes;
        }else {
            return new byte[1];
        }
    }

    public static byte[] encodeMessage(MidiMessage msg){
        byte[] result;
        switch (msg.type){
            case "note_off":
                result = _encode_note_off(msg);
                break;
            case "note_on":
                result = _encode_note_on(msg);
                break;
            default:
                result = new byte[]{(byte) (192 | msg.channel), (byte) msg.program};
        }
        return result;
    }

    public static byte[] _encode_note_off(MidiMessage msg){
        return new byte[]{(byte) (0x80 | msg.channel), (byte) msg.note, (byte) msg.velocity};
    }

    public static byte[] _encode_note_on(MidiMessage msg){
        return new byte[]{(byte) (0x90 | msg.channel), (byte) msg.note, (byte) msg.velocity};
    }
}
