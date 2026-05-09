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

import bisq.core.dao.DaoSetupService;
import bisq.core.dao.state.DaoStateListener;
import bisq.core.dao.state.DaoStateService;
import bisq.core.util.JsonUtil;

import bisq.common.config.Config;
import bisq.common.file.JsonFileManager;

import com.google.inject.Inject;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BurningManDataExportService implements DaoSetupService, DaoStateListener {
    private static final String FILE_NAME = "burning_man_data";
    private static final int SCHEMA_VERSION = 1;

    private final DaoStateService daoStateService;
    private final BurningManService burningManService;
    private final boolean dumpBurningManData;
    private final JsonFileManager jsonFileManager;

    @Inject
    public BurningManDataExportService(DaoStateService daoStateService,
                                       BurningManService burningManService,
                                       @Named(Config.STORAGE_DIR) File storageDir,
                                       @Named(Config.DUMP_BURNING_MAN_DATA) boolean dumpBurningManData) {
        this.daoStateService = daoStateService;
        this.burningManService = burningManService;
        this.dumpBurningManData = dumpBurningManData;
        jsonFileManager = new JsonFileManager(storageDir);
    }

    @Override
    public void addListeners() {
        if (dumpBurningManData) {
            daoStateService.addDaoStateListener(this);
        }
    }

    @Override
    public void start() {
        maybeExport();
    }

    @Override
    public void onParseBlockChainComplete() {
        maybeExport();
    }

    private void maybeExport() {
        if (!dumpBurningManData || !daoStateService.isParseBlockChainComplete()) {
            return;
        }

        int chainHeight = daoStateService.getChainHeight();
        int burningManSelectionHeight = DelayedPayoutTxReceiverService.getSnapshotHeight(
                daoStateService.getGenesisBlockHeight(),
                chainHeight,
                DelayedPayoutTxReceiverService.SNAPSHOT_SELECTION_GRID_SIZE,
                DelayedPayoutTxReceiverService.MIN_SNAPSHOT_HEIGHT);
        List<BurningManDataEntry> entries = getEntries(burningManSelectionHeight);
        BurningManData burningManData = new BurningManData(SCHEMA_VERSION,
                Config.baseCurrencyNetwork().name(),
                chainHeight,
                burningManSelectionHeight,
                burningManService.getLegacyBurningManAddress(burningManSelectionHeight),
                entries);

        jsonFileManager.writeToDisc(JsonUtil.objectToJson(burningManData), FILE_NAME);
        log.info("Exported {} Burning Man receiver entries to {}.json", entries.size(), FILE_NAME);
    }

    private List<BurningManDataEntry> getEntries(int chainHeight) {
        Map<String, Double> shareByAddress = new TreeMap<>();
        burningManService.getActiveBurningManCandidates(chainHeight).stream()
                .filter(candidate -> candidate.getReceiverAddress().isPresent())
                .forEach(candidate -> {
                    String address = candidate.getReceiverAddress().get();
                    shareByAddress.merge(address, candidate.getCappedBurnAmountShare(), Double::sum);
                });

        return shareByAddress.entrySet().stream()
                .map(entry -> new BurningManDataEntry(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private static class BurningManData {
        private final int schemaVersion;
        private final String baseCurrencyNetwork;
        private final int chainHeight;
        private final int burningManSelectionHeight;
        private final String legacyBurningManAddress;
        private final List<BurningManDataEntry> entries;

        private BurningManData(int schemaVersion,
                               String baseCurrencyNetwork,
                               int chainHeight,
                               int burningManSelectionHeight,
                               String legacyBurningManAddress,
                               List<BurningManDataEntry> entries) {
            this.schemaVersion = schemaVersion;
            this.baseCurrencyNetwork = baseCurrencyNetwork;
            this.chainHeight = chainHeight;
            this.burningManSelectionHeight = burningManSelectionHeight;
            this.legacyBurningManAddress = legacyBurningManAddress;
            this.entries = entries;
        }
    }

    private static class BurningManDataEntry {
        private final String receiverAddress;
        private final double cappedBurnAmountShare;

        private BurningManDataEntry(String receiverAddress, double cappedBurnAmountShare) {
            this.receiverAddress = receiverAddress;
            this.cappedBurnAmountShare = cappedBurnAmountShare;
        }
    }
}
