package org.interledger.stream.sender.good;

import org.interledger.core.Immutable;

import com.google.common.primitives.UnsignedLong;

/**
 * A container object that captures amounts used in auth, rollback, and commit operations inside of the Stream Sender.
 */
@Immutable
public interface PrepareAmounts {

  static PrepareAmountsBuilder builder() {
    return new PrepareAmountsBuilder();
  }

  /**
   * The amount that should be sent in a prepare packet for purposes of STREAM.
   *
   * @return An {@link UnsignedLong} that represents the `amount` value in an ILPv4 Prepare packet when used by a STREAM
   *     sender.
   */
  UnsignedLong amountToSend();

  /**
   * The amount, in receiver's units, that should be put into a Stream Packet's amount field to indicate the minimum
   * amount a receiver should accept for a given Stream Packet payment.
   *
   * @return An {@link UnsignedLong} that represents the minimum `amount` that a Stream Receiver should accept in an
   *     individual Stream Packet.
   */
  UnsignedLong minimumAmountToAccept();

}