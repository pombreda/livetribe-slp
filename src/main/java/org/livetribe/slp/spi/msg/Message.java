/*
 * Copyright 2005 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.livetribe.slp.spi.msg;

import org.livetribe.slp.ServiceLocationException;
import org.livetribe.slp.Attributes;

/**
 * The RFC 2608 message header is the following:
 * <pre>
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |    Version    |  Function-ID  |            Length             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * | Length, contd.|O|F|R|       reserved          |Next Ext Offset|
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  Next Extension Offset, contd.|              XID              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |      Language Tag Length      |         Language Tag          \
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * </pre>
 *
 * @version $Rev$ $Date$
 */
public abstract class Message extends BytesBlock
{
    public static final byte SRV_RQST_TYPE = 1;
    public static final byte SRV_RPLY_TYPE = 2;
    public static final byte SRV_REG_TYPE = 3;
    public static final byte SRV_DEREG_TYPE = 4;
    public static final byte SRV_ACK_TYPE = 5;
    public static final byte ATTR_RQST_TYPE = 6;
    public static final byte ATTR_RPLY_TYPE = 7;
    public static final byte DA_ADVERT_TYPE = 8;
    public static final byte SRV_TYPE_RQST_TYPE = 9;
    public static final byte SRV_TYPE_RPLY_TYPE = 10;
    public static final byte SA_ADVERT_TYPE = 11;

    private static final byte SLP_VERSION = 2;

    private boolean overflow;
    private boolean fresh;
    private boolean multicast;
    private int xid;
    private String language;

    protected abstract byte[] serializeBody() throws ServiceLocationException;

    protected abstract void deserializeBody(byte[] bytes) throws ServiceLocationException;

    public abstract byte getMessageType();

    public boolean isMulticast()
    {
        return multicast;
    }

    public void setMulticast(boolean multicast)
    {
        this.multicast = multicast;
    }

    public boolean isOverflow()
    {
        return overflow;
    }

    public void setOverflow(boolean overflow)
    {
        this.overflow = overflow;
    }

    public boolean isFresh()
    {
        return fresh;
    }

    public void setFresh(boolean fresh)
    {
        this.fresh = fresh;
    }

    public int getXID()
    {
        return xid;
    }

    public void setXID(int xid)
    {
        this.xid = xid;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public byte[] serialize() throws ServiceLocationException
    {
        byte[] body = serializeBody();

        byte[] languageBytes = stringToBytes(getLanguage());
        int headerLength = 14 + languageBytes.length;
        byte[] result = new byte[headerLength + body.length];

        int offset = 0;
        result[0] = SLP_VERSION;

        ++offset;
        result[1] = getMessageType();

        ++offset;
        int lengthBytes = 3;
        writeInt(result.length, result, offset, lengthBytes);

        offset += lengthBytes;
        int flagsBytes = 2;
        int flags = 0;
        if (isOverflow()) flags |= 0x8000;
        if (isFresh()) flags |= 0x4000;
        if (isMulticast()) flags |= 0x2000;
        writeInt(flags, result, offset, flagsBytes);

        // Ignore extensions, for now
        offset += flagsBytes;
        int extensionBytes = 3;
        writeInt(0, result, offset, extensionBytes);

        offset += extensionBytes;
        int xidBytes = 2;
        writeInt(getXID(), result, offset, xidBytes);

        offset += xidBytes;
        int languageLengthBytes = 2;
        writeInt(languageBytes.length, result, offset, languageLengthBytes);

        offset += languageLengthBytes;
        System.arraycopy(languageBytes, 0, result, offset, languageBytes.length);

        offset += languageBytes.length;
        System.arraycopy(body, 0, result, offset, body.length);

        return result;
    }

    /**
     * Parses the header of SLP messages, then each message parses its body via {@link #deserializeBody(byte[])}.
     * @throws ServiceLocationException If the bytes cannot be parsed
     */
    public static Message deserialize(byte[] bytes) throws ServiceLocationException
    {
        try
        {
            int offset = 0;
            byte version = bytes[offset];
            if (version != SLP_VERSION)
                throw new ServiceLocationException("Unsupported SLP version " + version + ", only version " + SLP_VERSION + " is supported", ServiceLocationException.PARSE_ERROR);

            ++offset;
            byte messageType = bytes[offset];

            ++offset;
            int lengthBytes = 3;
            int length = readInt(bytes, offset, lengthBytes);
            if (bytes.length != length)
                throw new ServiceLocationException("Expected message length is " + length + ", got instead " + bytes.length, ServiceLocationException.PARSE_ERROR);

            offset += lengthBytes;
            int flagsBytes = 2;
            int flags = readInt(bytes, offset, flagsBytes);

            // Ignore extensions, for now
            offset += flagsBytes;
            int extensionBytes = 3;
            readInt(bytes, offset, extensionBytes);

            offset += extensionBytes;
            int xidBytes = 2;
            int xid = readInt(bytes, offset, xidBytes);

            offset += xidBytes;
            int languageLengthBytes = 2;
            int languageLength = readInt(bytes, offset, languageLengthBytes);

            offset += languageLengthBytes;
            String language = readString(bytes, offset, languageLength);

            Message message = createMessage(messageType);
            message.setOverflow((flags & 0x8000) == 0x8000);
            message.setFresh((flags & 0x4000) == 0x4000);
            message.setMulticast((flags & 0x2000) == 0x2000);
            message.xid = xid;
            message.language = language;

            offset += languageLength;
            byte[] body = new byte[length - offset];
            System.arraycopy(bytes, offset, body, 0, body.length);
            message.deserializeBody(body);

            return message;
        }
        catch (IndexOutOfBoundsException x)
        {
            throw new ServiceLocationException(x, ServiceLocationException.PARSE_ERROR);
        }
    }

    private static Message createMessage(byte messageType) throws ServiceLocationException
    {
        switch (messageType)
        {
            case SRV_RQST_TYPE:
                return new SrvRqst();
            case SRV_RPLY_TYPE:
                return new SrvRply();
            case SRV_REG_TYPE:
                return new SrvReg();
            case SRV_DEREG_TYPE:
                return new SrvDeReg();
            case SRV_ACK_TYPE:
                return new SrvAck();
            case ATTR_RQST_TYPE:
                break;
            case ATTR_RPLY_TYPE:
                break;
            case DA_ADVERT_TYPE:
                return new DAAdvert();
            case SRV_TYPE_RQST_TYPE:
                break;
            case SRV_TYPE_RPLY_TYPE:
                break;
            case SA_ADVERT_TYPE:
                return new SAAdvert();
        }
        throw new ServiceLocationException("Unknown message " + messageType, ServiceLocationException.PARSE_ERROR);
    }

    protected static byte[] attributesToBytes(Attributes attributes) throws ServiceLocationException
    {
        if (attributes == null) return EMPTY_BYTES;
        return stringToUTF8Bytes(attributes.asString());
    }
}
