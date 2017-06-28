package com.khronodragon.bluestone.util;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TByteList;
import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TByteLinkedList;
import gnu.trove.list.linked.TIntLinkedList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.ArrayUtils;

public class Base65536 {
    private static final int bmpThreshold = 1 << 16;
    private static final int offset = 1 << 10;
    private static final int high = 0xd800;
    private static final int low = 0xdc00;
    private static final int possibleBytes = 1 << 8;

    private static final int paddingBlockStart = Strings.spread("ᔀ")[0];
    private static final int[] blockStarts = Strings.spread("㐀㔀㘀㜀㠀㤀㨀㬀㰀㴀㸀㼀䀀䄀䈀䌀" +
            "䐀䔀䘀䜀䠀䤀䨀䬀䰀一伀倀儀刀匀吀" +
            "唀嘀圀堀夀娀嬀尀崀帀开怀愀戀挀搀" +
            "攀昀最栀椀樀欀氀洀渀漀瀀焀爀猀琀" +
            "甀瘀眀砀礀稀笀簀紀縀缀耀脀舀茀萀" +
            "蔀蘀蜀蠀褀言謀谀贀踀輀退鄀鈀錀鐀" +
            "销阀需頀餀騀鬀鰀鴀鸀ꄀꈀꌀꔀ𐘀𒀀" +
            "𒄀𒈀𓀀𓄀𓈀𓌀𔐀𔔀𖠀𖤀𠀀𠄀𠈀𠌀𠐀𠔀" +
            "𠘀𠜀𠠀𠤀𠨀𠬀𠰀𠴀𠸀𠼀𡀀𡄀𡈀𡌀𡐀𡔀" +
            "𡘀𡜀𡠀𡤀𡨀𡬀𡰀𡴀𡸀𡼀𢀀𢄀𢈀𢌀𢐀𢔀" +
            "𢘀𢜀𢠀𢤀𢨀𢬀𢰀𢴀𢸀𢼀𣀀𣄀𣈀𣌀𣐀𣔀" +
            "𣘀𣜀𣠀𣤀𣨀𣬀𣰀𣴀𣸀𣼀𤀀𤄀𤈀𤌀𤐀𤔀" +
            "𤘀𤜀𤠀𤤀𤨀𤬀𤰀𤴀𤸀𤼀𥀀𥄀𥈀𥌀𥐀𥔀" +
            "𥘀𥜀𥠀𥤀𥨀𥬀𥰀𥴀𥸀𥼀𦀀𦄀𦈀𦌀𦐀𦔀" +
            "𦘀𦜀𦠀𦤀𦨀𦬀𦰀𦴀𦸀𦼀𧀀𧄀𧈀𧌀𧐀𧔀" +
            "𧘀𧜀𧠀𧤀𧨀𧬀𧰀𧴀𧸀𧼀𨀀𨄀𨈀𨌀𨐀𨔀");

    private static final TIntIntMap b2s = new TIntIntHashMap() {{
        for (int b = 0; b < possibleBytes; b++)
            put(blockStarts[b], b);
    }};

    public static String encode(byte[] bytes) {
        TIntList codePoints = new TIntLinkedList();

        for (int i = 0; i < bytes.length; i += 2) {
            int b1 = bytes[i];
            int blockStart = i + 1 < bytes.length ? blockStarts[bytes[i + 1]] : paddingBlockStart;
            int codePoint = blockStart + b1;
            codePoints.add(codePoint);
        }

        TIntIterator iter = codePoints.iterator();
        StringBuilder builder = new StringBuilder();

        while (iter.hasNext()) {
            int codePoint = iter.next();

            if (codePoint < bmpThreshold)
                builder.appendCodePoint(codePoint);

            int first = high + ((codePoint - bmpThreshold) / offset);
            int second = low + (codePoint % offset);

            builder.appendCodePoint(first)
                    .appendCodePoint(second);
        }

        return builder.toString();
    }

    public static byte[] decode(String str) throws DecoderException {
        int[] spread = Strings.spread(str);
        byte[] array = new byte[spread.length];
        boolean done = false;

        int i = 0;
        for (int codePoint: spread) {
            int b1 = codePoint & (possibleBytes - 1);
            int blockStart = codePoint - b1;

            if (blockStart == paddingBlockStart) {
                if (done)
                    throw new DecoderException("Base65536 sequence continued after final byte");

                array[i] = (byte) b1;
                done = true;
            } else {
                int b2 = b2s.get(blockStart);

                if (b2 != b2s.getNoEntryValue()) {
                    if (done)
                        throw new DecoderException("Base65536 sequence continued after final byte");

                    array[i] = (byte) b1;
                    array[++i] = (byte) b2;
                } else {
                    throw new DecoderException("Not a valid Base65536 codepoint: " + codePoint);
                }
            }

            i++;
        }

        return array;
    }
}
