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

package bisq.core.dao.burningman;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BurningManAddressListServiceTest {
    @Test
    void loadsBundledAddressListsByVersion() {
        BurningManAddressListService service = new BurningManAddressListService();

        assertEquals(List.of(1), service.getSupportedVersions());

        BurningManAddressList addressList = service.getAddressList(1);
        assertEquals(1, addressList.getListVersion());
        assertEquals("BTC_MAINNET", addressList.getNetwork());
        assertEquals(20, addressList.getReceiverAddresses().size());
        assertTrue(addressList.getAllowedAddresses().contains("34VLFgtFKAtwTdZ5rengTT2g2zC99sWQLC"));
    }

    @Test
    void selectsHighestCommonVersion() {
        BurningManAddressListService service = new BurningManAddressListService();

        assertEquals(1, service.selectHighestCommonVersion(List.of(1)));
    }

    @Test
    void rejectsUnsortedPeerVersions() {
        assertThrows(IllegalArgumentException.class,
                () -> BurningManAddressListService.getValidatedSupportedVersions(List.of(2, 1)));
    }

    @Test
    void rejectsEmptyPeerVersions() {
        assertThrows(IllegalArgumentException.class,
                () -> BurningManAddressListService.getValidatedSupportedVersions(List.of()));
    }

    @Test
    void rejectsDuplicatePeerVersions() {
        assertThrows(IllegalArgumentException.class,
                () -> BurningManAddressListService.getValidatedSupportedVersions(List.of(1, 1)));
    }

    @Test
    void rejectsNonPositivePeerVersions() {
        assertThrows(IllegalArgumentException.class,
                () -> BurningManAddressListService.getValidatedSupportedVersions(List.of(0)));
        assertThrows(IllegalArgumentException.class,
                () -> BurningManAddressListService.getValidatedSupportedVersions(List.of(-1)));
    }
}
