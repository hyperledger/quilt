package org.interledger.stream;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.primitives.UnsignedLong;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.fx.Denomination;
import org.interledger.stream.crypto.Random;
import org.interledger.stream.crypto.SharedSecret;
import org.interledger.stream.crypto.StreamEncryptionUtils;
import org.interledger.stream.frames.ConnectionAssetDetailsFrame;
import org.interledger.stream.frames.ConnectionCloseFrame;
import org.interledger.stream.frames.ConnectionNewAddressFrame;
import org.interledger.stream.frames.StreamCloseFrame;
import org.interledger.stream.frames.StreamFrame;
import org.interledger.stream.frames.StreamFrameType;
import org.interledger.stream.frames.StreamMoneyMaxFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for helping interact with Stream packets.
 */
// TODO: Unit test
public class StreamPacketUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(StreamPacketUtils.class);

  /**
   * The string "ilp_stream_fulfillment" is encoded as UTF-8 or ASCII (the byte representation is the same with both
   * encodings).
   */
  private static final byte[] ILP_STREAM_FULFILLMENT = "ilp_stream_fulfillment".getBytes(StandardCharsets.UTF_8);

  private static final String HMAC_SHA256_ALG_NAME = "HmacSHA256";

  public static final UnsignedLong DEFAULT_STREAM_ID = UnsignedLong.ONE;

  /**
   * If the sender does not want the receiver to be able to fulfill the payment (as for an informational quote), they
   * can generate an unfulfillable random condition.
   *
   * @return A {@link InterledgerCondition} that is not fulfillable.
   */
  public static InterledgerCondition unfulfillableCondition() {
    return InterledgerCondition.of(Random.randBytes(32));
  }

  /**
   * <p>If the sender _does_ want the receiver to be able to fulfill the condition, the condition MUST be generated
   * from a fulfillment in the following manner: First, the shared_secret is the cryptographic seed exchanged during
   * Setup. The string "ilp_stream_fulfillment" is encoded as UTF-8 or ASCII (the byte representation is the same with
   * both encodings). Finally, the data is the encrypted STREAM packet.</p>
   *
   * @param sharedSecret The cryptographic seed exchanged during STREAM Setup.
   * @param data         The encrypted STREAM packet in ASN.1 OER bytes.
   * @return An {@link InterledgerFulfillment} that can be used to prove a payment.
   */
  public static InterledgerFulfillment generatedFulfillableFulfillment(
    final SharedSecret sharedSecret, final byte[] data
  ) {
    Objects.requireNonNull(sharedSecret);
    Objects.requireNonNull(data);

    // hmac_key = hmac_sha256(shared_secret, "ilp_stream_fulfillment");
    final SecretKey secretKey = new SecretKeySpec(sharedSecret.key(), HMAC_SHA256_ALG_NAME);
    final byte[] hmacKey = Hashing.hmacSha256(secretKey).hashBytes(ILP_STREAM_FULFILLMENT).asBytes();

    // fulfillment = hmac_sha256(hmac_key, data);
    final SecretKey hmacSecretKey = new SecretKeySpec(hmacKey, HMAC_SHA256_ALG_NAME);
    final byte[] fulfillmentBytes = Hashing.hmacSha256(hmacSecretKey).hashBytes(data).asBytes();

    return InterledgerFulfillment.of(fulfillmentBytes);
  }

  public static Optional<StreamPacket> mapToStreamPacket(
    final byte[] ilpPacketData,
    final SharedSecret sharedSecret,
    final StreamEncryptionUtils streamEncryptionUtils
  ) {
    try {
      return Optional.ofNullable(ilpPacketData)
        .filter(data -> data.length > 0) // <-- Ensures we don't even try with empty data payloads.
        .map(data -> streamEncryptionUtils.fromEncrypted(sharedSecret, data));
    } catch (Exception e) {
      LOGGER.error(
        "Unable to decrypt ILP response packet's data. packetData={}", ilpPacketData, e
      );
      return Optional.empty();
    }
  }

  // TODO: Javadoc
  public static Optional<StreamPacket> getStreamPacket(final InterledgerResponsePacket responsePacket) {
    Objects.requireNonNull(responsePacket);
    return responsePacket.typedData()
      .filter(typedData -> StreamPacket.class.isAssignableFrom(typedData.getClass()))
      .map($ -> (StreamPacket) $);
  }

//  // TODO: Used? Maybe use this everywhere and try typedData, and if that doesn't work, then try decrypting?
//  // TODO: See StreamUtils in Connector too.
//  public static Optional<StreamPacket> mapToStreamPacket(
//    final byte[] packetData,
//    final SharedSecret sharedSecret,
//    final StreamEncryptionUtils streamEncryptionUtils
//  ) {
//    return packet.typedData()
//      .filter($ -> $.getClass().isAssignableFrom(StreamPacket.class))
//      .map($ -> (StreamPacket) $)
//      .map(Optional::of)
//      .orElseGet(() -> {
//        try {
//          return Optional.ofNullable(packet.getData())
//            .filter(data -> data.length > 0) // <-- Ensures we don't even try with empty data payloads.
//            .map(data -> streamEncryptionUtils.fromEncrypted(sharedSecret, data));
//        } catch (Exception e) {
//          LOGGER.error(
//            "Unable to decrypt ILP response packet's data. packet={}", packet, e
//          );
//          return Optional.empty();
//        }
//      });
//  }

//  public static Optional<StreamPacket> constructStreamPacket(
//    final InterledgerPreparePacket packet,
//    final SharedSecret sharedSecret,
//    final StreamEncryptionUtils streamEncryptionUtils
//  ) {
//
//  }

//  // TODO: Used? Maybe use this everywhere and try typedData, and if that doesn't work, then try decrypting?
//  // TODO: See StreamUtils in Connector too.
//  public static InterledgerResponsePacket withStreamPacket(
//    final InterledgerResponsePacket responsePacket,
//    final SharedSecret sharedSecret,
//    final StreamEncryptionUtils streamEncryptionUtils
//  ) {
//    return responsePacket.map(
//      interledgerFulfillPacket -> mapToStreamPacket(interledgerFulfillPacket, sharedSecret, streamEncryptionUtils)
//        .map(streamPacket -> InterledgerFulfillPacket.builder()
//          .from(interledgerFulfillPacket)
//          .typedData(streamPacket)
//          .build())
//        .orElse((AbstractInterledgerFulfillPacket) interledgerFulfillPacket),
//      interledgerRejectPacket -> mapToStreamPacket(interledgerRejectPacket, sharedSecret, streamEncryptionUtils)
//        .map(streamPacket -> InterledgerRejectPacket.builder()
//          .from(interledgerRejectPacket)
//          .typedData(streamPacket)
//          .build())
//        .orElse((AbstractInterledgerRejectPacket) interledgerRejectPacket));
//  }

  public static final Set<StreamFrameType> CLOSING_FRAMES = Sets.newHashSet(
    StreamFrameType.ConnectionClose,
    StreamFrameType.StreamClose
  );

  public static Optional<Denomination> getDenomination(StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);

    return streamPacket.frames().stream()
      .filter(frame -> frame.streamFrameType().equals(StreamFrameType.ConnectionAssetDetails))
      .map(frame -> ((ConnectionAssetDetailsFrame) frame))
      .map(ConnectionAssetDetailsFrame::sourceDenomination)
      .findFirst();
  }

  public static boolean hasCloseFrame(StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return hasCloseFrame(streamPacket.frames());
  }

  public static boolean hasCloseFrame(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .map(StreamFrame::streamFrameType)
      .anyMatch(CLOSING_FRAMES::contains);
  }

  // TODO: Unit tests
  public static Optional<ConnectionCloseFrame> findConnectionCloseFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findConnectionCloseFrame(streamPacket.frames());
  }

  // TODO: Unit tests
  public static Optional<ConnectionCloseFrame> findConnectionCloseFrame(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.ConnectionClose)
      .filter(streamFrame -> ConnectionCloseFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (ConnectionCloseFrame) $)
      .findFirst();
  }

  // TODO: Unit tests
  public static Optional<StreamCloseFrame> findStreamCloseFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findStreamCloseFrame(streamPacket.frames());
  }

  // TODO: Unit tests
  public static Optional<StreamCloseFrame> findStreamCloseFrame(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.StreamClose)
      .filter(streamFrame -> StreamCloseFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (StreamCloseFrame) $)
      .findFirst();
  }

  // TODO: Unit tests
  public static Optional<ConnectionAssetDetailsFrame> findConnectionAssetDetailsFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findConnectionAssetDetailsFrame(streamPacket.frames());
  }

  public static Optional<ConnectionAssetDetailsFrame> findConnectionAssetDetailsFrame(
    final Collection<StreamFrame> streamPacketFrames
  ) {
    Objects.requireNonNull(streamPacketFrames);
    return streamPacketFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.ConnectionAssetDetails)
      .filter(streamFrame -> ConnectionAssetDetailsFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (ConnectionAssetDetailsFrame) $)
      .findFirst();
  }

  // TODO: Unit tests
  public static long countConnectionAssetDetailsFrame(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return streamPacket.frames().stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.ConnectionAssetDetails)
      .filter(streamFrame -> ConnectionAssetDetailsFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (ConnectionAssetDetailsFrame) $)
      .count();
  }

  // TODO: Unit tests
  public static Collection<StreamMoneyMaxFrame> findStreamMaxMoneyFrames(final StreamPacket streamPacket) {
    Objects.requireNonNull(streamPacket);
    return findStreamMaxMoneyFrames(streamPacket.frames());
  }

  // TODO: Unit tests
  public static Collection<StreamMoneyMaxFrame> findStreamMaxMoneyFrames(final Collection<StreamFrame> streamFrames) {
    Objects.requireNonNull(streamFrames);
    return streamFrames.stream()
      .filter((streamFrame) -> streamFrame.streamFrameType() == StreamFrameType.StreamMoneyMax)
      .filter(streamFrame -> StreamMoneyMaxFrame.class.isAssignableFrom(streamFrame.getClass()))
      .map($ -> (StreamMoneyMaxFrame) $)
      .collect(Collectors.toList());
  }

  // TODO: Unit tests
  public static Optional<InterledgerAddress> findSourceAddress(StreamPacket streamPacket) {
    return streamPacket.frames().stream()
      .filter(frame -> frame.streamFrameType().equals(StreamFrameType.ConnectionNewAddress))
      .map(frame -> ((ConnectionNewAddressFrame) frame).sourceAddress())
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

  /**
   * An {@link InterledgerFulfillPacket} is considered authentic because we assume that a receiver would not have
   * fulfilled if they were unable to decrypt packets.
   *
   * @param interledgerFulfillPacket
   * @return
   */
  @Deprecated
  // TODO: Used?
  public static boolean isAuthentic(final InterledgerFulfillPacket interledgerFulfillPacket) {
    Objects.requireNonNull(interledgerFulfillPacket);
    return true;
  }

  /**
   * An {@link InterledgerRejectPacket} is considered to be authentic if a stream packet can be decrypted from the ILP
   * packet.
   *
   * @param interledgerRejectPacket
   * @return
   */
  // TODO: Used? See StreamPacketReply instead.
  @Deprecated
  public static boolean isAuthentic(final InterledgerRejectPacket interledgerRejectPacket) {
    Objects.requireNonNull(interledgerRejectPacket);

    return interledgerRejectPacket.typedData()
      .filter($ -> StreamPacket.class.isAssignableFrom($.getClass()))
      .map($ -> (StreamPacket) $)
      .map(Optional::of)
      .map(streamPacket -> true)
      .orElse(false);
  }
}
