package io.particle.particlemesh.meshsetup.connection.security

import io.particle.ecjpake4j.ECJPakeImpl
import io.particle.ecjpake4j.Role
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnection
import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.meshsetup.connection.BlePacket
import io.particle.particlemesh.meshsetup.connection.InboundFrameReader
import io.particle.particlemesh.meshsetup.connection.OutboundFrameWriter
import kotlinx.coroutines.experimental.launch


class CryptoDelegateFactory {

    suspend fun createCryptoDelegate(
            meshSetupConnection: MeshSetupConnection,
            frameWriter: OutboundFrameWriter,
            frameReader: InboundFrameReader,
            jpakeLowEntropyPassword: String
    ): AesCcmDelegate? {

        val transceiver = JpakeExchangeMessageTransceiver(frameWriter, frameReader)
        val jpakeManager = JpakeExchangeManager(
                ECJPakeImpl(Role.CLIENT, jpakeLowEntropyPassword),
                transceiver
        )

        val jpakeMgrChannel = meshSetupConnection.packetReceiveChannel
        launch {
            for (packet in jpakeMgrChannel) {
                QATool.runSafely({ frameReader.receivePacket(BlePacket(packet)) })
            }
        }

        val jpakeSecret = jpakeManager.performJpakeExchange()

        return AesCcmDelegate.newDelegateFromJpakeSecret(jpakeSecret)
    }

}