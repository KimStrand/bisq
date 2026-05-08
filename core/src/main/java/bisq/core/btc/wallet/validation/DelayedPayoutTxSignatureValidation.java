/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.btc.wallet.validation;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.SignatureDecodeException;

import java.math.BigInteger;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class DelayedPayoutTxSignatureValidation {
    private DelayedPayoutTxSignatureValidation() {
    }

    public static ECKey.ECDSASignature checkCanonicalDelayedPayoutTxSignature(byte[] signature,
                                                                              String signer) {
        byte[] checkedSignature = checkNotNull(signature,
                "%s delayed payout tx signature must not be null",
                signer);
        try {
            ECKey.ECDSASignature decodedSignature = ECKey.ECDSASignature.decodeFromDER(checkedSignature);
            checkArgument(Arrays.equals(checkedSignature, decodedSignature.encodeToDER()),
                    "%s delayed payout tx signature must be strictly DER encoded",
                    signer);
            checkArgument(isValidSignatureValue(decodedSignature.r),
                    "%s delayed payout tx signature r value is outside of allowed range",
                    signer);
            checkArgument(isValidSignatureValue(decodedSignature.s),
                    "%s delayed payout tx signature s value is outside of allowed range",
                    signer);
            checkArgument(decodedSignature.isCanonical(),
                    "%s delayed payout tx signature must be canonical (low-S)",
                    signer);
            return decodedSignature;
        } catch (SignatureDecodeException e) {
            throw new IllegalArgumentException("Invalid " + signer + " delayed payout tx signature", e);
        }
    }

    private static boolean isValidSignatureValue(BigInteger value) {
        return value.signum() > 0 && value.compareTo(ECKey.CURVE.getN()) < 0;
    }
}
