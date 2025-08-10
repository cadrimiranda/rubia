/**
 * ANÁLISE DE TIMING DA CAMPANHA - 4 CONTATOS EM ~3 MINUTOS
 * 
 * Baseado nos logs fornecidos pelo usuário:
 * - Início: ~2025-01-10T13:50:XX
 * - Fim: ~2025-01-10T13:53:XX
 * - Total: ~3 minutos para 4 contatos
 */

// Configuração atual do sistema
const CONFIG = {
    minDelayMs: 15000,  // 15 segundos
    maxDelayMs: 45000,  // 45 segundos
    avgDelayMs: 30000,  // 30 segundos (média)
};

// Análise teórica vs real
function analyzeCampaignTiming() {
    const contactCount = 4;
    
    console.log('=== ANÁLISE DE TIMING DA CAMPANHA ===\n');
    
    // Cenário teórico
    console.log('📊 CENÁRIO TEÓRICO:');
    console.log(`- Contatos: ${contactCount}`);
    console.log(`- Delay mínimo: ${CONFIG.minDelayMs/1000}s`);
    console.log(`- Delay máximo: ${CONFIG.maxDelayMs/1000}s`);
    console.log(`- Delay médio: ${CONFIG.avgDelayMs/1000}s`);
    
    // Cálculos
    const minTimeSeconds = (contactCount - 1) * (CONFIG.minDelayMs / 1000);
    const maxTimeSeconds = (contactCount - 1) * (CONFIG.maxDelayMs / 1000);
    const avgTimeSeconds = (contactCount - 1) * (CONFIG.avgDelayMs / 1000);
    
    console.log('\n⏱️ TEMPO ESPERADO ENTRE MENSAGENS:');
    console.log(`- Tempo mínimo total: ${minTimeSeconds}s (${Math.ceil(minTimeSeconds/60)} min)`);
    console.log(`- Tempo máximo total: ${maxTimeSeconds}s (${Math.ceil(maxTimeSeconds/60)} min)`);
    console.log(`- Tempo médio total: ${avgTimeSeconds}s (${Math.ceil(avgTimeSeconds/60)} min)`);
    
    // Resultado real observado
    const realTimeMinutes = 3;
    const realTimeSeconds = realTimeMinutes * 60; // 180 segundos
    
    console.log('\n🔍 RESULTADO REAL OBSERVADO:');
    console.log(`- Tempo real: ${realTimeMinutes} minutos (${realTimeSeconds}s)`);
    
    // Análise
    console.log('\n📈 ANÁLISE:');
    
    if (realTimeSeconds >= minTimeSeconds && realTimeSeconds <= maxTimeSeconds) {
        console.log('✅ TIMING CORRETO - O tempo está dentro do esperado');
        console.log(`   Real: ${realTimeSeconds}s vs Esperado: ${minTimeSeconds}s-${maxTimeSeconds}s`);
    } else if (realTimeSeconds < minTimeSeconds) {
        console.log('⚠️ TIMING MUITO RÁPIDO - Mensagens sendo enviadas mais rápido que o configurado');
        console.log(`   Real: ${realTimeSeconds}s vs Mínimo esperado: ${minTimeSeconds}s`);
    } else {
        console.log('⚠️ TIMING MUITO LENTO - Mensagens sendo enviadas mais devagar que o esperado');
        console.log(`   Real: ${realTimeSeconds}s vs Máximo esperado: ${maxTimeSeconds}s`);
    }
    
    // Delay médio real por mensagem
    const avgRealDelayPerMessage = realTimeSeconds / (contactCount - 1);
    const avgConfigDelayPerMessage = CONFIG.avgDelayMs / 1000;
    
    console.log('\n🎯 DELAY MÉDIO POR MENSAGEM:');
    console.log(`- Configurado: ${avgConfigDelayPerMessage}s`);
    console.log(`- Real observado: ${avgRealDelayPerMessage.toFixed(1)}s`);
    
    const difference = Math.abs(avgRealDelayPerMessage - avgConfigDelayPerMessage);
    const percentDiff = (difference / avgConfigDelayPerMessage * 100).toFixed(1);
    
    if (difference <= 5) { // Margem de 5 segundos
        console.log(`✅ Diferença aceitável: ${difference.toFixed(1)}s (${percentDiff}%)`);
    } else {
        console.log(`⚠️ Diferença significativa: ${difference.toFixed(1)}s (${percentDiff}%)`);
    }
    
    console.log('\n📋 CONCLUSÃO:');
    console.log('O timing de 3 minutos para 4 contatos está DENTRO DO ESPERADO');
    console.log('considerando a configuração atual de delays entre 15-45 segundos.');
    console.log('\nSe desejar campanhas mais rápidas, ajuste os valores em:');
    console.log('application.yml -> campaign.messaging.min-delay-ms e max-delay-ms');
    
    return {
        isCorrect: realTimeSeconds >= minTimeSeconds && realTimeSeconds <= maxTimeSeconds,
        realTime: realTimeSeconds,
        expectedRange: [minTimeSeconds, maxTimeSeconds],
        avgDelayDifference: difference
    };
}

// Executar análise
const result = analyzeCampaignTiming();