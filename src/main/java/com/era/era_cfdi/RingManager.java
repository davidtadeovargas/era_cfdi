/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era.era_cfdi;

import com.era.models.BasDats;
import com.era.models.Company;
import com.era.models.ImpuestosXVenta;
import com.era.models.Partvta;
import com.era.models.Sales;
import com.era.models.Tax;
import com.era.models.Unid;
import com.era.repositories.RepositoryFactory;
import com.era.utilities.UtilitiesFactory;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import xmlcfdi.Cfdi;

/**
 *
 * @author PC
 */
public class RingManager {
    
    private static RingManager RingManager;
    
    private final String temportalXMLGeneration = UtilitiesFactory.getSingleton().getFilesUtility().getCurrentWorkingDir() + "\\tmp.xml";
    
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
                                    final BigDecimal totalImpuestosTrasladados,
                                    final boolean testMode) throws Exception {
                
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
        
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        final String fecha = dateFormat.format(Sale.getFalt()).replace(" ", "T");
        
        cfdi.setAtributo("Fecha",fecha);
        if(!trasladado){
            cfdi.setAtributo("MetodoPago", Sale.getPaymentMethod());
            cfdi.setAtributo("FormaPago",Sale.getPaymentForm());
        }
        cfdi.setAtributo("Moneda", Sale.getCoinCode());
        cfdi.setAtributo("TipoCambio", String.valueOf((int)changeType));
        cfdi.setAtributo("TipoDeComprobante", tipoComprobante);
        cfdi.setAtributo("LugarExpedicion", Sale.getExpeditionPlace());
        
        cfdi.emisor().setAtributo("Rfc", BasDats.getRFC());
        cfdi.emisor().setAtributo("Nombre", BasDats.getNom());
        cfdi.emisor().setAtributo("RegimenFiscal", BasDats.getRegfisc());
        
        cfdi.receptor().setAtributo("Rfc", Customer.getRFC());
        cfdi.receptor().setAtributo("Nombre", Customer.getNom());
        cfdi.receptor().setAtributo("UsoCFDI", Sale.getUsocfdi());
        
        if (totalImpuestosRetenidos.compareTo(BigDecimal.ZERO) > 0 && !trasladado) {
            cfdi.impuestos().setAtributo("TotalImpuestosRetenidos", totalImpuestosRetenidos.toString());
        }
        if(totalImpuestosTrasladados.compareTo(BigDecimal.ZERO) > 0 && !trasladado){
            cfdi.impuestos().setAtributo("TotalImpuestosTrasladados", totalImpuestosTrasladados.toString());
        }
        
        final BigDecimal totalDecimal = Sale.getTotal().setScale(2, BigDecimal.ROUND_HALF_EVEN);
        
        if(trasladado){
            cfdi.setAtributo("SubTotal", Sale.getSubtotal().toString());
            if (Sale.getTotalDisccount().compareTo(BigDecimal.ZERO) > 0){
                cfdi.setAtributo("Descuento", Sale.getTotalDisccount().toString());
            }
            cfdi.setAtributo("Total", totalDecimal.toString());
        }else{
            cfdi.setAtributo("SubTotal", Sale.getSubtotal().toString());
            if (Sale.getTotalDisccount().compareTo(BigDecimal.ZERO) > 0){
                cfdi.setAtributo("Descuento", totalDecimal.toString());
            }
            cfdi.setAtributo("Total", totalDecimal.toString());
        }
        
        int contadorConcepto = 0;
        
        //Get all the part items from the sale
        final List<Partvta> items = RepositoryFactory.getInstance().getPartvtaRepository().getPartsVta(Sale.getId());
        for(Partvta Partvta_:items){
            
            final Unid Unid = (Unid)RepositoryFactory.getInstance().getUnidsRepository().getByCode(Partvta_.getUnid());
            
            cfdi.conceptos().concepto(contadorConcepto).setAtributo("ClaveProdServ", Partvta_.getKeySAT());
            cfdi.conceptos().concepto(contadorConcepto).setAtributo("Cantidad", String.valueOf(Partvta_.getCant()));
            cfdi.conceptos().concepto(contadorConcepto).setAtributo("ClaveUnidad", Unid.getClaveSAT());
            cfdi.conceptos().concepto(contadorConcepto).setAtributo("Descripcion", Partvta_.getDescrip());
            cfdi.conceptos().concepto(contadorConcepto).setAtributo("ValorUnitario", Partvta_.getPre().toString());
            
            if(Partvta_.getDescu().compareTo(BigDecimal.ZERO)>0){
                cfdi.conceptos().concepto(contadorConcepto).setAtributo("Descuento", Partvta_.getDescu().toString());
            }
            
            cfdi.conceptos().concepto(contadorConcepto).setAtributo("Importe", Partvta_.getImpo().toString());
            
            int contadorTraslado = 0;
            int contadorRetencion = 0;
            
            //Get all the taxes from the part item
            final List<ImpuestosXVenta> taxes = RepositoryFactory.getInstance().getImpuestosXVentasRepository().getAllBySaleId(Partvta_.getVta());
            for(ImpuestosXVenta ImpuestosXVenta_:taxes){
                
                final Tax Tax = (Tax)RepositoryFactory.getInstance().getTaxesRepository().getByCode(ImpuestosXVenta_.getImpuesto());
                
                final String tasaCuota = String.valueOf(Tax.getValue() / 100);
                BigDecimal importeBigDecimal = Partvta_.getPre().multiply(BigDecimal.valueOf(Double.valueOf(tasaCuota)));
                importeBigDecimal = importeBigDecimal.setScale(2, BigDecimal.ROUND_HALF_EVEN);
                final String importe = String.valueOf(importeBigDecimal);
                final String base = String.valueOf((importeBigDecimal.multiply(Partvta_.getCant())).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                
                if(ImpuestosXVenta_.isRetencion()){
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Retenciones().Retencion(contadorRetencion).setAtributo("Base",base);
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Retenciones().Retencion(contadorRetencion).setAtributo("Impuesto", ImpuestosXVenta_.getImpuesto());
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Retenciones().Retencion(contadorRetencion).setAtributo("TipoFactor", "Tasa");
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Retenciones().Retencion(contadorRetencion).setAtributo("TasaOCuota",tasaCuota);
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Retenciones().Retencion(contadorRetencion).setAtributo("Importe", importe);
                }
                else{
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Traslados().Traslado(contadorTraslado).setAtributo("Base", base);
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Traslados().Traslado(contadorTraslado).setAtributo("Impuesto", ImpuestosXVenta_.getImpuesto());
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Traslados().Traslado(contadorTraslado).setAtributo("TipoFactor", "Tasa");
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Traslados().Traslado(contadorTraslado).setAtributo("TasaOCuota",tasaCuota);
                    cfdi.conceptos().concepto(contadorConcepto).impuestos().Traslados().Traslado(contadorTraslado).setAtributo("Importe", importe);
                    
                    ++contadorTraslado;
                    ++contadorRetencion;
                }
            }
            
            ++contadorConcepto;
        }
        
        //Create parts items
        
        cfdi.setCertificado(BasDats.getRutcer());
        
        if(!trasladado){
            //sCreImpXML33(total_impuesto_trasladado, total_impuesto_retenido, retencionesCfdi, trasladosCfdi,cfdi);
        }
        
        final String rutCert = BasDats.getRutcer();
        cfdi.sellarCFDI("cadenaoriginal_3_3.xslt", BasDats.getRutkey(), BasDats.getPasscer());
        
        cfdi.timbradoPrueba(testMode);
        
        cfdi.timbrar(BasDats.getRFC());
        
        final ResultRing ResultRing = new ResultRing();
        ResultRing.setTransactionID(cfdi.getsTransactionID().replace("\"",""));
        ResultRing.setSello(cfdi.getsSelloDigital().replace("\"",""));
        ResultRing.setCertificateSAT(cfdi.getNoCertificadoSat().replace("\"",""));
        //ResultRing.setSello(cfdi.getSelloSAT().replace("\"",""));
        ResultRing.setFiscalFolio(cfdi.getUUID().replace("\"",""));
        ResultRing.setXml(cfdi.getXMLflujo());
        ResultRing.setRingedDate(cfdi.getFechaTimbrado().replace("\"",""));
        ResultRing.setVersion("3.3");
        ResultRing.setOriginalString("||" + ResultRing.getVersion() + "|" + ResultRing.getFiscalFolio() + "|" + ResultRing.getRingedDate() + "|" + ResultRing.getSello() + "|" + ResultRing.getCertificateSAT() + "||");
        
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
