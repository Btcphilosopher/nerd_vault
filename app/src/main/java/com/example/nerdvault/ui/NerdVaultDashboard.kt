package com.example.nerdvault.ui

import kotlin.math.absoluteValue
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nerdvault.data.*
import com.example.nerdvault.domain.AssetType
import com.example.nerdvault.viewmodel.NerdVaultViewModel

// Cosmic High Contrast Cyberpunk Colors
val TechBlack = Color(0xFE0E1216)
val ShadowGrey = Color(0xFF161F26)
val CyberGreen = Color(0xFF00FF66)
val CyberGreenAlpha = Color(0x3300FF66)
val CyberCyan = Color(0xFF00E5FF)
val HighContrastText = Color(0xFFFFFFFF)
val DimGold = Color(0xFFFFD700)
val DarkBorder = Color(0x44FFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NerdVaultDashboard(viewModel: NerdVaultViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    
    // States from VM
    val primaryUser by viewModel.currentUserState.collectAsStateWithLifecycle()
    val assets by viewModel.assetsState.collectAsStateWithLifecycle()
    val marketListings by viewModel.activeListingsState.collectAsStateWithLifecycle()
    val creators by viewModel.creatorsState.collectAsStateWithLifecycle()
    val merchCatalog by viewModel.merchCatalogState.collectAsStateWithLifecycle()
    val eventPasses by viewModel.eventPassesState.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogState.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val opStatus by viewModel.operationStatus.collectAsStateWithLifecycle()

    // Dialog control triggers
    var activeQrDialogTicket by remember { mutableStateOf<AssetEntity?>(null) }
    var activeDirectMintDialog by remember { mutableStateOf(false) }
    var activeTransferDialogAsset by remember { mutableStateOf<AssetEntity?>(null) }

    // Navigation configuration
    val navItems = listOf(
        NavigationItem("Wallet", Icons.Filled.AccountBalanceWallet),
        NavigationItem("Store & Tickets", Icons.Filled.Storefront),
        NavigationItem("P2P Market", Icons.Filled.SwapHoriz),
        NavigationItem("Creator Hub", Icons.Filled.Groups),
        NavigationItem("API Sandbox", Icons.Filled.Terminal),
        NavigationItem("Analytics & Ledger", Icons.Filled.History)
    )

    // Clear alert handler banner automatically
    LaunchedEffect(opStatus) {
        if (opStatus != null) {
            kotlinx.coroutines.delay(4500)
            viewModel.clearStatus()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack)
    ) {
        val isWideScreen = maxWidth > 680.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = "Logo",
                                tint = CyberGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NERDVAULT",
                                color = HighContrastText,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 20.sp,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x3300FF66)),
                                border = BorderStroke(1.dp, Color(0x6600FF66)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "V1.0 API CORE",
                                    color = CyberGreen,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ShadowGrey,
                        titleContentColor = HighContrastText
                    ),
                    actions = {
                        primaryUser?.let { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = "XP",
                                    tint = DimGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "LVL ${user.xp / 1000} (${user.xp} XP)",
                                    color = DimGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                // Adaptive layout: render BottomNav on compact mobile
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = ShadowGrey,
                        tonalElevation = 8.dp
                    ) {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(item.icon, contentDescription = item.label, tint = if (selectedTab == index) TechBlack else HighContrastText) },
                                label = { Text(item.label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TechBlack,
                                    selectedTextColor = CyberGreen,
                                    indicatorColor = CyberGreen,
                                    unselectedIconColor = HighContrastText,
                                    unselectedTextColor = HighContrastText
                                )
                            )
                        }
                    }
                }
            },
            containerColor = TechBlack
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Wide Screen Side Navigation Rail
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = ShadowGrey,
                        header = {
                            Spacer(modifier = Modifier.height(12.dp))
                            IconButton(onClick = { activeDirectMintDialog = true }) {
                                Icon(Icons.Filled.Add, contentDescription = "Mint Asset", tint = CyberGreen)
                            }
                        },
                        modifier = Modifier.width(100.dp)
                    ) {
                        navItems.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, fontSize = 9.sp, textAlign = TextAlign.Center) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = TechBlack,
                                    selectedTextColor = CyberGreen,
                                    indicatorColor = CyberGreen,
                                    unselectedIconColor = HighContrastText,
                                    unselectedTextColor = HighContrastText
                                )
                            )
                        }
                    }
                    VerticalDivider(color = DarkBorder, thickness = 1.dp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    // Main Routing Layouts based on tabs
                    Crossfade(targetState = selectedTab, label = "tabTrans") { tab ->
                        when (tab) {
                            0 -> WalletScreen(
                                user = primaryUser,
                                assets = assets,
                                recommendations = recommendations,
                                onOpenQrTicket = { activeQrDialogTicket = it },
                                onOpenTransfer = { activeTransferDialogAsset = it },
                                onOpenMint = { activeDirectMintDialog = true }
                            )
                            1 -> CommerceScreen(
                                merchList = merchCatalog,
                                passes = eventPasses,
                                userBalance = primaryUser?.creditsBalance ?: 0.0,
                                onBuyMerch = { viewModel.buyMerchandise(it.id) },
                                onBuyPass = { viewModel.buyEventPass(it.id) }
                            )
                            2 -> MarketplaceScreen(
                                listings = marketListings,
                                userAddress = primaryUser?.walletAddress ?: "",
                                creditsBalance = primaryUser?.creditsBalance ?: 0.0,
                                onPurchase = { viewModel.buyMarketplaceItem(it.id) },
                                onBidSubmit = { listId, bid -> viewModel.bidOnAuction(listId, bid) }
                            )
                            3 -> CreatorScreen(
                                creators = creators,
                                userAssets = assets,
                                onSubscribe = { viewModel.subscribeCreatorClub(it.id) }
                            )
                            4 -> ApiSandboxScreen(
                                viewModel = viewModel,
                                user = primaryUser
                            )
                            5 -> AnalyticsLedgerScreen(
                                auditLogs = auditLogs
                            )
                        }
                    }

                    // Floating Notification toast
                    opStatus?.let { statusMsg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                            border = BorderStroke(2.dp, CyberGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = CyberGreen,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = statusMsg,
                                    color = HighContrastText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.clearStatus() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Dialogs ---

        // 1. QR Validator Ticket Entry Code Simulator
        activeQrDialogTicket?.let { ticket ->
            Dialog(onDismissRequest = { activeQrDialogTicket = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                    border = BorderStroke(1.dp, CyberGreen),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PASS ENTRY VERIFY",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = ticket.name,
                            color = HighContrastText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Simulated high contrast modern QR block widget
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            QrSimulationCanvas(ticket.id)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ticket Code: ${ticket.id}",
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                viewModel.validateAccessTicket(ticket.id)
                                activeQrDialogTicket = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = TechBlack)
                        ) {
                            Text("Simulator Scan Barcode")
                        }
                    }
                }
            }
        }

        // 2. Transfer asset dialog
        activeTransferDialogAsset?.let { asset ->
            var recipientInput by remember { mutableStateOf("0x44bfbc690cd42ea2d6f21c5c0a32194b1a89c92b") }
            Dialog(onDismissRequest = { activeTransferDialogAsset = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                    border = BorderStroke(1.dp, CyberCyan)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Secure Wallet Transfer",
                            color = HighContrastText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Asset: ${asset.name}", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = recipientInput,
                            onValueChange = { recipientInput = it },
                            label = { Text("Recipient Wallet Address (0x)", color = Color.LightGray) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, color = HighContrastText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color.LightGray,
                                focusedLabelColor = CyberCyan
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { activeTransferDialogAsset = null }) {
                                Text("Cancel", color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.executeApiSandboxRequest(
                                        "/wallet/transfer", "POST",
                                        "{\"assetId\":\"${asset.id}\",\"recipient\":\"$recipientInput\"}"
                                    )
                                    activeTransferDialogAsset = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = TechBlack)
                            ) {
                                Text("Execute Transfer")
                            }
                        }
                    }
                }
            }
        }

        // 3. Direct Creator Minting Dialog
        if (activeDirectMintDialog) {
            var assetNameInput by remember { mutableStateOf("Alpha Strike Skin") }
            var assetValInput by remember { mutableStateOf("450") }
            var assetTypeSelected by remember { mutableStateOf(AssetType.COSMETIC) }

            Dialog(onDismissRequest = { activeDirectMintDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                    border = BorderStroke(1.dp, CyberGreen)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Creator Minting Portal",
                            color = HighContrastText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Deploys any new programmable asset into the ecosystem database.",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = assetNameInput,
                            onValueChange = { assetNameInput = it },
                            label = { Text("Asset Title", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = Color.LightGray,
                                focusedTextColor = HighContrastText
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = assetValInput,
                            onValueChange = { assetValInput = it },
                            label = { Text("Base System Valuation (Credits)", color = Color.LightGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = Color.LightGray,
                                focusedTextColor = HighContrastText
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Select Classification:", color = HighContrastText, fontSize = 12.sp)
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            AssetType.values().take(3).forEach { type ->
                                val isSelected = assetTypeSelected == type
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (isSelected) CyberGreen else TechBlack),
                                    border = BorderStroke(1.dp, if (isSelected) CyberGreen else Color.Gray),
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .clickable { assetTypeSelected = type }
                                ) {
                                    Text(
                                        type.name,
                                        color = if (isSelected) TechBlack else Color.LightGray,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { activeDirectMintDialog = false }) {
                                Text("Discard", color = Color.LightGray)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val credits = assetValInput.toDoubleOrNull() ?: 50.0
                                    viewModel.mintAssetDirectly(assetNameInput, credits, assetTypeSelected)
                                    activeDirectMintDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = TechBlack)
                            ) {
                                Text("Mint to DB")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Data Navigation Holder
data class NavigationItem(val label: String, val icon: ImageVector)

// --- INDIVIDUAL TAB CONTROLLERS UI ---

// 1. Wallets Screen Dashboard
@Composable
fun WalletScreen(
    user: UserEntity?,
    assets: List<AssetEntity>,
    recommendations: List<com.example.nerdvault.domain.Asset>,
    onOpenQrTicket: (AssetEntity) -> Unit,
    onOpenTransfer: (AssetEntity) -> Unit,
    onOpenMint: () -> Unit
) {
    if (user == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = CyberGreen)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Powering up cryptography engines...", color = HighContrastText)
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            // High contrast cosmic credit/score block
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(2.dp, CyberGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "UNIFIED NERD WALLET PORTAL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Address: ${user.walletAddress}",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SYSTEM CREDITS", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                "${user.creditsBalance} $",
                                color = CyberGreen,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = onOpenMint,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = TechBlack),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Filled.BuildCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mint Asset", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Multi-game Token exchange balances block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MultiCoinBalanceItem("Game Gold (RPG)", user.gamePointsGold.toString(), "🪙")
                        MultiCoinBalanceItem("V-Points (Shooter)", user.gamePointsVBucks.toString(), "💎")
                        MultiCoinBalanceItem("Guild Coins (Clan)", user.gamePointsGuild.toString(), "⚜️")
                    }
                }
            }
        }

        item {
            // Intelligent Recommendation engine suggested cards matching requirement 6
            AnimatedVisibility(visible = recommendations.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = CyberCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "INTELLIGENT RECOMMENDATION DECK",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        recommendations.take(2).forEach { rec ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                                border = BorderStroke(1.dp, CyberCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        rec.type.name,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        rec.name,
                                        fontSize = 12.sp,
                                        color = HighContrastText,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        rec.description,
                                        fontSize = 10.sp,
                                        color = Color.LightGray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${rec.valueInCredits} Credits",
                                        color = CyberGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                "MY OWNED PROGRAMMABLE ASSETS",
                color = HighContrastText,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (assets.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Inbox, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Assets Owned Yet.", color = Color.Gray, fontSize = 13.sp)
                        Text("Mine some coins or purchase collectibles in Store to begin!", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(assets) { asset ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                    border = BorderStroke(0.5.dp, if (asset.stateType == "REDEEMED") Color.DarkGray else CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0x2200E5FF)),
                                    border = BorderStroke(0.5.dp, CyberCyan)
                                ) {
                                    Text(
                                        asset.type,
                                        color = CyberCyan,
                                        fontSize = 8.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when (asset.stateType) {
                                            "REDDMD" -> Color.DarkGray
                                            "REDEEMED" -> Color.Red
                                            "LISTED" -> Color(0xFFE65100)
                                            else -> Color(0xFF0D5330)
                                        }
                                    )
                                ) {
                                    Text(
                                        asset.stateType,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(asset.name, color = HighContrastText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(asset.description, color = Color.LightGray, fontSize = 11.sp)
                        }

                        // Actions for individual assets
                        Row {
                            if (asset.type == "PASSPORT_TICKET" && asset.stateType != "REDEEMED") {
                                IconButton(onClick = { onOpenQrTicket(asset) }) {
                                    Icon(Icons.Filled.QrCode, contentDescription = "Scan QR Ticket", tint = CyberGreen)
                                }
                            }
                            if (asset.isTransferable && asset.stateType == "OWNED") {
                                IconButton(onClick = { onOpenTransfer(asset) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Transfer", tint = CyberCyan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiCoinBalanceItem(label: String, valStr: String, prefix: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 9.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(prefix, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(3.dp))
            Text(valStr, color = HighContrastText, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

// 2. Commerce Screen Code (Figurines & Tickets)
@Composable
fun CommerceScreen(
    merchList: List<MerchCatalogEntity>,
    passes: List<EventPassEntity>,
    userBalance: Double,
    onBuyMerch: (MerchCatalogEntity) -> Unit,
    onBuyPass: (EventPassEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "COMMERCE CENTER (PHYSICAL + DIGITAL)",
                fontWeight = FontWeight.Bold,
                color = CyberGreen,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Section 1: Event Tickets (Pass System)
        item {
            Text("EVENT PASSPORTS & esports TOURS", color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (passes.isEmpty()) {
            item { Text("No active esports ticket events found.", color = Color.Gray, fontSize = 11.sp) }
        } else {
            items(passes) { pass ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                    border = BorderStroke(1.dp, Color(0xFF9C27B0)), // Dark Violet ticker
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pass.eventName, color = HighContrastText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${pass.ticketPrice} Credits", color = CyberGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Text(pass.description, color = Color.LightGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(pass.dateString, color = Color.Gray, fontSize = 10.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(pass.location, color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                            Button(
                                onClick = { onBuyPass(pass) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0), contentColor = Color.White),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("Securing Ticket", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Physical Merch Store
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text("OFFICIAL BRAND MERCHANDISE (SHOPIFY INJECT)", color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(merchList) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(0.5.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (item.category) {
                                "APPAREL" -> "👕"
                                "PERIPHERALS" -> "⌨️"
                                "COLLECTIBLE" -> "🗿"
                                else -> "📦"
                            },
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(item.description, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("In Stock: ${item.stockCount} left", color = Color.Yellow, fontSize = 9.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${item.price} $", color = CyberGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Button(
                            onClick = { onBuyMerch(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = TechBlack),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("BUY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 3. Marketplace Screen Trades
@Composable
fun MarketplaceScreen(
    listings: List<MarketListingEntity>,
    userAddress: String,
    creditsBalance: Double,
    onPurchase: (MarketListingEntity) -> Unit,
    onBidSubmit: (String, Double) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "P2P TRADE COURT & STEAM MARKETPLACE",
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (listings.isEmpty()) {
            item {
                Text("No open peer listings currently. Check back later!", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            items(listings) { listing ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                    border = BorderStroke(1.dp, if (listing.isAuction) Color(0xFFFF9800) else CyberCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = if (listing.isAuction) Color(0x33FF9800) else Color(0x3300E5FF))
                                    ) {
                                        Text(
                                            if (listing.isAuction) "ACTIVE AUCTION" else "FIXED PRICE",
                                            color = if (listing.isAuction) Color(0xFFFF9800) else CyberCyan,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(listing.assetName, color = HighContrastText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Seller: ${listing.sellerAddress}", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${if (listing.isAuction) listing.highestBid else listing.sellPrice} Credits",
                                    color = CyberGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text("Listed price", color = Color.LightGray, fontSize = 8.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            if (listing.isAuction) {
                                var customBidInput by remember { mutableStateOf((listing.highestBid + 25.0).toString()) }
                                OutlinedTextField(
                                    value = customBidInput,
                                    onValueChange = { customBidInput = it },
                                    label = { Text("Your Bid", color = Color.LightGray, fontSize = 8.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberGreen,
                                        unfocusedBorderColor = Color.LightGray,
                                        focusedTextColor = HighContrastText
                                    ),
                                    modifier = Modifier
                                        .width(90.dp)
                                        .height(46.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onBidSubmit(listing.id, customBidInput.toDoubleOrNull() ?: 0.0) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800), contentColor = TechBlack)
                                ) {
                                    Text("PLACE BID", fontSize = 10.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onPurchase(listing) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = TechBlack)
                                ) {
                                    Text("COLLECT NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. Creator Screen Code (Fan Club Patreon)
@Composable
fun CreatorScreen(
    creators: List<CreatorEntity>,
    userAssets: List<AssetEntity>,
    onSubscribe: (CreatorEntity) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "CREATOR ECONOMY COOPERATIVE (PATREON INJECT)",
                fontWeight = FontWeight.Bold,
                color = CyberGreen,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(creators) { creator ->
            // Check if user is already a member of this fanbase
            val isMember = userAssets.any { it.type == "CREATOR_MEMBERSHIP" && it.creatorId == creator.id }
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(1.dp, if (isMember) CyberGreen else Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE91E63), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(creator.name.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(creator.name, color = HighContrastText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Split Share Config: 85% Creator / 15% Vault", color = Color.LightGray, fontSize = 9.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Fan tags:", color = Color.Gray, fontSize = 10.sp)
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        creator.fandomTags.split(",").forEach { tag ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF)),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    tag,
                                    fontSize = 9.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${creator.totalSubscribers} Fans subscribed", color = Color.LightGray, fontSize = 10.sp)
                            Text("Total Earnings: ${creator.accumulatedEarnings} $", color = CyberGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        if (isMember) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0x3300FF66)),
                                border = BorderStroke(1.dp, CyberGreen)
                            ) {
                                Text(
                                    "SUBSCRIBED MEMBER",
                                    color = CyberGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { onSubscribe(creator) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63), contentColor = Color.White),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("UNIFY PATREON ACCESS (150 $)", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. REST API INTERACTIVE SANDBOX HUB
@Composable
fun ApiSandboxScreen(
    viewModel: NerdVaultViewModel,
    user: UserEntity?
) {
    var routeRequest by remember { mutableStateOf("/wallet/transfer") }
    var methodSelected by remember { mutableStateOf("POST") }
    var jsonPayloadInput by remember { mutableStateOf("""{
  "assetId": "nft_cyb_skn_01",
  "recipient": "0x44bfbc690cd42ea2d6f21c5c0a32194b1a89c92b",
  "comment": "Trade transfer for Apex Skin"
}""".trimIndent()) }

    // Endpoints specifications catalogue
    val endpointsList = listOf(
        EndpointSpec("POST", "/wallet/create", """{"username":"NerdLover","email":"nerd@ahyx.org"}"""),
        EndpointSpec("POST", "/wallet/transfer", """{"assetId":"nft_cyb_skn_01","recipient":"0x44bfbc690cd42ea2d6f21c5c0a32194b1a89c92b"}"""),
        EndpointSpec("POST", "/assets/mint", """{"name":"Alpha Saber","type":"COSMETIC","value":150.0}"""),
        EndpointSpec("POST", "/market/list", """{"assetId":"nft_guild_crest","price":200.0}"""),
        EndpointSpec("POST", "/store/checkout", """{"itemId":"merch_art_keycap"}"""),
        EndpointSpec("POST", "/tickets/mint", """{"eventId":"evt_arena_championship"}""")
    )

    val apiResult by viewModel.sandboxResponse.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "NERDVAULT PROGRAMMABLE API ENGINE",
                fontWeight = FontWeight.Bold,
                color = CyberGreen,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "Interact and play with the actual live local backend REST gateway routes directly using JSON structures.",
                color = Color.LightGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Available API Methods Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(0.5.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("ENDPOINT BLUEPRINTS (Tap to copy/populate payload)", color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    endpointsList.forEach { endpoint ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    routeRequest = endpoint.path
                                    methodSelected = endpoint.method
                                    jsonPayloadInput = endpoint.payloadTemplate
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (endpoint.method == "POST") Color(0xFF1B5E20) else Color(0xFF0D47A1))
                            ) {
                                Text(
                                    endpoint.method,
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(endpoint.path, color = CyberGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Active request constructor
        item {
            Text("REST INPUT PANEL", color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(1.dp, CyberGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CyberGreen),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                methodSelected,
                                color = TechBlack,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = routeRequest,
                            onValueChange = { routeRequest = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = HighContrastText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberGreen,
                                unfocusedBorderColor = Color.DarkGray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("JSON REQUEST BODY:", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = jsonPayloadInput,
                        onValueChange = { jsonPayloadInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = HighContrastText),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberGreen,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.executeApiSandboxRequest(routeRequest, methodSelected, jsonPayloadInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = TechBlack),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DISPATCH REST CALL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Live response logger terminal
        item {
            Text("GATEWAY RESPONSE MONITOR (LOG)", color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, Color.Gray),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (apiResult == null) {
                        Text(
                            "Terminal idle. Dispatch an API route request above to dump transaction JSON packets here...",
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    } else {
                        val res = apiResult!!
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "HTTP/1.1 ${res.statusCode} ${res.statusMessage}",
                                color = if (res.statusCode < 300) CyberGreen else Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                "LAT: ${res.executionTimeMs}ms",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Headers:", color = Color.Yellow, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        res.headers.forEach { (k, v) ->
                            Text("$k: $v", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Body packets:", color = Color.Yellow, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x11FFFFFF))
                                .border(0.5.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                res.bodyJson,
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(res.bodyJson))
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy JSON to clipboard", tint = CyberGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        // Dynamic HTML Embed component simulator as requested by requirement 9
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text("EMBEDDABLE WEB COMPONENTS (JS WIDGET)", color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(0.5.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("HTML embedding code snippet to render Store checkout widgets into third-party games or blogs:", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(8.dp)
                    ) {
                        val buildCode = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFE040FB))) { append("<div ") }
                            withStyle(SpanStyle(color = Color(0xFF29B6F6))) { append("data-nerdvault-store") }
                            withStyle(SpanStyle(color = Color.White)) { append("=") }
                            withStyle(SpanStyle(color = Color(0xFFA5D6A7))) { append("\"game_apex_viper_1\"") }
                            withStyle(SpanStyle(color = Color(0xFFE040FB))) { append("></div>\n") }
                            withStyle(SpanStyle(color = Color(0xFFE040FB))) { append("<script ") }
                            withStyle(SpanStyle(color = Color(0xFF29B6F6))) { append("src") }
                            withStyle(SpanStyle(color = Color.White)) { append("=") }
                            withStyle(SpanStyle(color = Color(0xFFA5D6A7))) { append("\"https://api.nerdvault.com/widget.js\"") }
                            withStyle(SpanStyle(color = Color(0xFFE040FB))) { append("></script>") }
                        }
                        Text(buildCode, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

data class EndpointSpec(val method: String, val path: String, val payloadTemplate: String)

// 6. SOURCED TRANSACTION AUDIT TRAILS & SYSTEM LOGS PAGE
@Composable
fun AnalyticsLedgerScreen(
    auditLogs: List<AuditEventEntity>
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "SECURED TRANSACTION LEDGER (AUDITORS SCREEN)",
                fontWeight = FontWeight.Bold,
                color = CyberGreen,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                "Fully transparent, immutable event sourced database block outputs. Real audit log packets.",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // TEXT BASED ARCH PATHWAY SYSTEM DESIGN FLOW
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowGrey),
                border = BorderStroke(0.5.dp, CyberGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "SYSTEM COOPERATIVE TOPOLOGY",
                        color = HighContrastText,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Simple structured text flowchart drawing
                    Text(
                        text = """
                          [ IDENTITY / OAUTH GATEWAY ]
                                     |
                                     v
                          [   API GATEWAY ROUTER   ]
                                     |
                        +------------+------------+
                        |                         |
                        v                         v
                  [ WALLET ENGINE ]       [ COMMERCE ENGINE ]
                        |                         |
                        v                         v
                  [ REVENUE SHARE]        [ ADAPTIVE EMITTED QR ]
                        |                         |
                        +------------+------------+
                                     |
                                     v
                          [ ROOM SQL LITE LEDGER ] 
                        """.trimIndent(),
                        color = CyberCyan,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        item {
            Text("EVENT SOURCE BLOCKS", color = HighContrastText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.height(6.dp))
        }

        if (auditLogs.isEmpty()) {
            item {
                Text("Ledger record list is currently empty.", color = Color.Gray, fontSize = 11.sp)
            }
        } else {
            items(auditLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                log.type,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = when (log.type) {
                                    "ASSET_MINTED" -> CyberGreen
                                    "ORDER_COMPLETED" -> CyberCyan
                                    "TICKET_REDEEMED" -> Color(0xFFE91E63)
                                    else -> Color.Yellow
                                },
                                fontSize = 10.sp
                            )
                            Text(
                                "BLOCK: #${log.eventId.hashCode().absoluteValue % 10000}",
                                color = Color.LightGray,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.summary, color = HighContrastText, fontSize = 11.sp)
                        Text("Actor: ${log.actorAddress}", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(11.dp), tint = CyberGreen)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Cryptographic SHA Signature: ${log.signature}",
                                color = CyberGreen,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom simulated QR block canvas
@Composable
fun QrSimulationCanvas(id: String) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val hash = id.hashCode().absoluteValue
        val stepsX = 6
        val stepsY = 6
        val cellW = size.width / stepsX
        val cellH = size.height / stepsY
        
        for (i in 0 until stepsX) {
            for (j in 0 until stepsY) {
                // Pseudo randomness based on ticket code hash to draw authentic custom QR pixel grid
                val fill = ((hash shr (i + j * 3)) % 2 != 0)
                if (fill || (i==0 && j==0) || (i==stepsX-1 && j==0) || (i==0 && j==stepsY-1)) {
                    drawRect(
                        color = Color.Black,
                        topLeft = androidx.compose.ui.geometry.Offset(i * cellW, j * cellH),
                        size = androidx.compose.ui.geometry.Size(cellW, cellH)
                    )
                }
            }
        }
    }
}
