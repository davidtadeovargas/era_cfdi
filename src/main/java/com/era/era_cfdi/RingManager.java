/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era.era_cfdi;

import com.era.models.BasDats;
import com.era.models.Company;
import com.era.models.Sales;
import com.era.utilities.UtilitiesFactory;
import java.io.File;
import java.math.BigDecimal;
import xmlcfdi.Cfdi;

/**
 *
 * @author PC
 */
public class RingManager {
    
    private static RingManager RingManager;
    
    private final String temportalXMLGeneration = UtilitiesFactory.getSingleton().getFilesUtility().getCurrentWorkingDir();
    
    public static RingManager getSingleton(){
        if(RingManager==null){
            RingManager = new RingManager();
        }
        return RingManager;
    }
    
    public ResultRing ringSale(     final Sales Sale,
                                    final BasDats BasDats,
                                    final Company Customer,
                                    final boolean trasladado,
                                    final float changeType,
                                    final String tipoComprobante,                                    
                                    final BigDecimal totalImpuestosRetenidos,
                                    final BigDecimal totalImpuestosTrasladados) throws Exception {
        
        Cfdi cfdi = new Cfdi();
        cfdi.setAtributo("xmlns:cfdi", "http://www.sat.gob.mx/cfd/3");
        cfdi.setAtributo("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        cfdi.setAtributo("xsi:schemaLocation", "http://www.sat.gob.mx/cfd/3 "
                    + "http://www.sat.gob.mx/sitio_internet/cfd/3/cfdv33.xsd ");            
        cfdi.setRutaGeneracionXML(temportalXMLGeneration);
        cfdi.setRutaXMLTimbrado(temportalXMLGeneration);
        cfdi.setAtributo("Version","3.3");
        cfdi.setAtributo("Serie", Sale.getSerie());
        cfdi.setAtributo("Folio", Sale.getReferenceNumber());
        cfdi.setAtributo("Fecha",Sale.getFalt().toString());
        if(!trasladado){
            cfdi.setAtributo("MetodoPago", Sale.getPaymentMethod());
            cfdi.setAtributo("FormaPago",Sale.getPaymentForm());
        }
        cfdi.setAtributo("Moneda", Sale.getCoinCode());
        cfdi.setAtributo("TipoCambio", String.valueOf(changeType));
        cfdi.setAtributo("TipoDeComprobante", tipoComprobante);
        cfdi.setAtributo("LugarExpedicion", Sale.getExpeditionPlace());
        
        cfdi.emisor().setAtributo("Rfc", BasDats.getRFC());
        cfdi.emisor().setAtributo("Nombre", BasDats.getNom());
        cfdi.emisor().setAtributo("RegimenFiscal", BasDats.getRegfisc());
        
        cfdi.receptor().setAtributo("Rfc", Customer.getRFC());
        cfdi.receptor().setAtributo("Nombre", Customer.getNom());
        cfdi.receptor().setAtributo("UsoCFDI", "");
        
        if (totalImpuestosRetenidos.compareTo(BigDecimal.ZERO) > 0 && !trasladado) {
            cfdi.impuestos().setAtributo("TotalImpuestosRetenidos", totalImpuestosRetenidos.toString());
        }
        if(totalImpuestosTrasladados.compareTo(BigDecimal.ZERO) > 0 && !trasladado){
            cfdi.impuestos().setAtributo("TotalImpuestosTrasladados", totalImpuestosTrasladados.toString());
        }
        
        if(trasladado){
            cfdi.setAtributo("SubTotal", "0.00");
            cfdi.setAtributo("Total", "0.00");
        }else{
            cfdi.setAtributo("SubTotal", Sale.getSubtotal().toString());
            if (Sale.getTotalDisccount().compareTo(BigDecimal.ZERO) > 0){
                cfdi.setAtributo("Descuento", Sale.getTotalDisccount().toString());
            }
            cfdi.setAtributo("Total", Sale.getTotal().toString());
        }
        
        cfdi.setCertificado(BasDats.getRutcer());
        
        if(!trasladado){
            //sCreImpXML33(total_impuesto_trasladado, total_impuesto_retenido, retencionesCfdi, trasladosCfdi,cfdi);
        }
        
        boolean test = false;
        if(Customer.getRFC().compareTo("LAN7008173R5") == 0)  
            test = true;
        
        cfdi.timbradoPrueba(test);
        
        //cfdi.sellarCFDI("cadenaoriginal_3_3.xslt", BasDats.getRutcer(), BasDats.getRutkey(), BasDats.getPasscer());
        
        //cfdi.timbrar(Customer.getRFC());
        
        final ResultRing ResultRing = new ResultRing();
        //ResultRing.setTransactionID(cfdi.getsTransactionID().replace("\"",""));
        //ResultRing.setSello(cfdi.getsSelloDigital().replace("\"",""));
        //ResultRing.setCertificateSAT(cfdi.getNoCertificadoSat().replace("\"",""));
        //ResultRing.setSello(cfdi.getSelloSAT().replace("\"",""));
        //ResultRing.setFiscalFolio(cfdi.getUUID().replace("\"",""));
        //ResultRing.setXml(cfdi.getXMLflujo());
        //ResultRing.setRingedDate(cfdi.getFechaTimbrado().replace("\"",""));
        //ResultRing.setVersion("3.3");
        //ResultRing.setOriginalString("||" + ResultRing.getVersion() + "|" + ResultRing.getFiscalFolio() + "|" + ResultRing.getRingedDate() + "|" + ResultRing.getSello() + "|" + ResultRing.getCertificateSAT() + "||");
        
        return ResultRing;
    }
    
    public interface OnResult{
        public void OnResult();
    }
    
    public class ResultRing{
        
        private String transactionID;
        private String sello;
        private String certificateSAT;
        private String fiscalFolio;
        private String xml;
        private String ringedDate;
        private String version;
        private String originalString;

        public String getTransactionID() {
            return transactionID;
        }

        public void setTransactionID(String transactionID) {
            this.transactionID = transactionID;
        }

        public String getSello() {
            return sello;
        }

        public void setSello(String sello) {
            this.sello = sello;
        }

        public String getCertificateSAT() {
            return certificateSAT;
        }

        public void setCertificateSAT(String certificateSAT) {
            this.certificateSAT = certificateSAT;
        }

        public String getFiscalFolio() {
            return fiscalFolio;
        }

        public void setFiscalFolio(String fiscalFolio) {
            this.fiscalFolio = fiscalFolio;
        }

        public String getXml() {
            return xml;
        }

        public void setXml(String xml) {
            this.xml = xml;
        }

        public String getRingedDate() {
            return ringedDate;
        }

        public void setRingedDate(String ringedDate) {
            this.ringedDate = ringedDate;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getOriginalString() {
            return originalString;
        }

        public void setOriginalString(String originalString) {
            this.originalString = originalString;
        }                
    }
}