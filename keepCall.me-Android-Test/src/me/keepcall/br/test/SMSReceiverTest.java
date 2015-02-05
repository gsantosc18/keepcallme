package me.keepcall.br.test;

import java.text.ParseException;
import java.util.Map;

import me.keepcall.br.SMSReceiver;

import org.mockito.Mockito;

import android.telephony.SmsMessage;
import android.test.AndroidTestCase;

public class SMSReceiverTest extends AndroidTestCase {

	SMSReceiver smsReceiver;
	Map<String, Integer> notificationMap;

	@Override
	protected void setUp() throws Exception {
		smsReceiver = new SMSReceiver();
		SMSReceiver.clearNotificationMap();
		notificationMap = smsReceiver.getNotificationMap();
	}

	public void testClaro() throws ParseException {

		SmsMessage smsMessage0 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage0.getDisplayMessageBody()).thenReturn("Claro Recado: o numero 06292691135 ligou e nao deixou recado 12/04-10:55. ...");
		smsReceiver.processSMS(smsMessage0, getContext());
		assertEquals(1, notificationMap.size());
		assertEquals(Integer.valueOf(1), notificationMap.get("Paula Simone"));

		SmsMessage smsMessage1 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage1.getDisplayMessageBody()).thenReturn(
				"Voce recebeu 1 ligacao de 06292691135, que nao deixou recado na Secretaria Claro. Ultima ligacao: 08/04, 12:34.\nClaro");
		smsReceiver.processSMS(smsMessage1, getContext());
		assertEquals(1, notificationMap.size());
		assertEquals(Integer.valueOf(2), notificationMap.get("Paula Simone"));

		SmsMessage smsMessage2 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage2.getDisplayMessageBody()).thenReturn(
				"Voce recebeu 1 ligacao de 06239429652, que nao deixou recado na Secretaria Claro. Ultima ligacao: 08/04, 12:34.\nClaro");
		smsReceiver.processSMS(smsMessage2, getContext());
		assertEquals(2, notificationMap.size());
		assertEquals(Integer.valueOf(2), notificationMap.get("Paula Simone"));
		assertEquals(Integer.valueOf(1), notificationMap.get("Casa"));

		SmsMessage smsMessage3 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage3.getDisplayMessageBody()).thenReturn(
				"Voce recebeu 2 ligacoes de 06239429652, que nao deixou recado na Secretaria Claro. Ultima ligacao: 08/04, 13:03.\nClaro");
		smsReceiver.processSMS(smsMessage3, getContext());
		assertEquals(2, notificationMap.size());
		assertEquals(Integer.valueOf(2), notificationMap.get("Paula Simone"));
		assertEquals(Integer.valueOf(3), notificationMap.get("Casa"));

		SmsMessage smsMessage4 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage4.getDisplayMessageBody()).thenReturn("Seu celular tem 1 ligação perdida de 06292691135, 02/06, 13:16. CLARO");
		smsReceiver.processSMS(smsMessage4, getContext());
		assertEquals(2, notificationMap.size());
		assertEquals(Integer.valueOf(3), notificationMap.get("Paula Simone"));
		assertEquals(Integer.valueOf(3), notificationMap.get("Casa"));

	}

	public void testTim() throws ParseException {

		SmsMessage smsMessage1 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage1.getDisplayMessageBody()).thenReturn("Voce recebeu ligacoes de: <08/04> 0416292691135 <1>  13:47hs");
		smsReceiver.processSMS(smsMessage1, getContext());
		assertEquals(1, notificationMap.size());
		assertEquals(Integer.valueOf(1), notificationMap.get("Paula Simone"));

		SmsMessage smsMessage2 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage2.getDisplayMessageBody()).thenReturn(
				"Voce recebeu ligacoes de: <08/04> 0416292691135 <2>  13:52hs 0416293193993 <1>  13:52hs 0416239429652 <1>  13:52hs");
		smsReceiver.processSMS(smsMessage2, getContext());
		assertEquals(3, notificationMap.size());
		assertEquals(Integer.valueOf(3), notificationMap.get("Paula Simone"));
		assertEquals(Integer.valueOf(1), notificationMap.get("Casa"));
		assertEquals(Integer.valueOf(1), notificationMap.get("Henrique Toda Via"));

	}

	public void testVivo() throws ParseException {

		SmsMessage smsMessage1 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage1.getDisplayMessageBody()).thenReturn(
				"Vivo Avisa:  o numero 06292691135 tentou ligar para voce, dia 08/04 as 14:23. Vivo. Conectados vivemos melhor.");
		smsReceiver.processSMS(smsMessage1, getContext());
		assertEquals(1, notificationMap.size());
		assertEquals(Integer.valueOf(1), notificationMap.get("Paula Simone"));

		SmsMessage smsMessage2 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage2.getDisplayMessageBody()).thenReturn(
				"Vivo Avisa:  o numero 06293193993 tentou ligar 12 vezes e a ultima vez foi, dia 08/04 as 14:28. Vivo. Conectados vivemos melhor.");
		smsReceiver.processSMS(smsMessage2, getContext());
		assertEquals(2, notificationMap.size());
		assertEquals(Integer.valueOf(1), notificationMap.get("Paula Simone"));
		assertEquals(Integer.valueOf(12), notificationMap.get("Henrique Toda Via"));

	}

	public void testOi() throws ParseException {

		SmsMessage smsMessage1 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage1.getDisplayMessageBody()).thenReturn("Seu Oi recebeu ligações de: 06292691135 08/04 16:45 <1>");
		smsReceiver.processSMS(smsMessage1, getContext());
		assertEquals(1, notificationMap.size());
		assertEquals(Integer.valueOf(1), notificationMap.get("Paula Simone"));

		SmsMessage smsMessage2 = Mockito.mock(SmsMessage.class);
		Mockito.when(smsMessage2.getDisplayMessageBody()).thenReturn(
				"Seu Oi recebeu ligações de: 06292691135 08/04 16:45 <1> 06293193993 08/04 17:00 <11>");
		smsReceiver.processSMS(smsMessage2, getContext());
		assertEquals(2, notificationMap.size());
		assertEquals(Integer.valueOf(2), notificationMap.get("Paula Simone"));
		assertEquals(Integer.valueOf(11), notificationMap.get("Henrique Toda Via"));

	}

}
