package uk.ac.ebi.pride.archive.pipeline.services.pia;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.jmzidml.model.mzidml.SpectraData;
import uk.ac.ebi.pride.archive.pipeline.utility.SubmissionPipelineConstants;
import uk.ac.ebi.pride.tools.jmzreader.JMzReader;
import uk.ac.ebi.pride.tools.jmzreader.JMzReaderException;
import uk.ac.ebi.pride.tools.jmzreader.model.Spectrum;
import uk.ac.ebi.pride.tools.mgf_parser.MgfFile;
import uk.ac.ebi.pride.tools.mzdata_wrapper.MzMlWrapper;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLFile;
import uk.ac.ebi.pride.tools.mzxml_parser.MzXMLParsingException;
import uk.ac.ebi.pride.tools.pkl_parser.PklFile;
import uk.ac.ebi.pride.tools.pride_wrapper.PRIDEXmlWrapper;
import uk.ac.ebi.pride.utilities.util.Triple;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class JmzReaderSpectrumService {

    /**
     * Map of all readers containing the spectra
     */
    Map<String, JMzReader> readers = new HashMap<>();

    String clientRegion = "*** Client region ***";
    String bucketName = "*** Bucket name ***";
    String key = "*** Object key ***";

    AmazonS3 s3Client;

    private JmzReaderSpectrumService(List<Triple<String, SpectraData, SubmissionPipelineConstants.FileType>> spectrumFileList) throws JMzReaderException, MzXMLParsingException {
        this.readers = new HashMap<>();
        for (Triple<String, SpectraData, SubmissionPipelineConstants.FileType> entry : spectrumFileList) {
            String key = (String) entry.getFirst();
            SubmissionPipelineConstants.FileType value = entry.getThird();
//            if(value == null && entry.getSecond().getSpectrumIDFormat().getCvParam().getAccession().equals("MS:1000774")){
//                entry.setThird(SubmissionPipelineConstants.FileType.MGF);
//                value = SubmissionPipelineConstants.FileType.MGF;
//            }
            if (value == SubmissionPipelineConstants.FileType.MGF) {
                this.readers.put(key, new MgfFile(new File(key), true));
            }
            if (value == SubmissionPipelineConstants.FileType.PRIDE) {
                this.readers.put(key, new PRIDEXmlWrapper(new File(key)));
            }
            if( value == SubmissionPipelineConstants.FileType.MZML){
                this.readers.put(key, new MzMlWrapper(new File(key)));
            }
            if( value == SubmissionPipelineConstants.FileType.PKL){
                this.readers.put(key, new PklFile(new File(key)));
            }
            if( value == SubmissionPipelineConstants.FileType.MZXML){
                this.readers.put(key, new MzXMLFile(new File(key)));
            }
        }
    }

    /**
     * Return an instance that allow to read the spectra from the original file.
     *
     * @param spectrumFileList
     * @return
     * @throws JMzReaderException
     */
    public static JmzReaderSpectrumService getInstance(List<Triple<String, SpectraData, SubmissionPipelineConstants.FileType>> spectrumFileList) throws JMzReaderException, MzXMLParsingException {
        return new JmzReaderSpectrumService(spectrumFileList);
    }

    public Spectrum getSpectrumById(String filePath, String id) throws JMzReaderException {
        JMzReader reader = readers.get(filePath);
        try{
            return reader.getSpectrumById(id);
        }catch (java.lang.NumberFormatException e){
            throw new JMzReaderException("Error parsing the following Accession -- " + id);
        }
    }

    public Spectrum getSpectrumByIndex(String filePath, int id) throws JMzReaderException {
        JMzReader reader = readers.get(filePath);
        try{
            return reader.getSpectrumByIndex(id);
        }catch (java.lang.NumberFormatException e){
            throw new JMzReaderException("Error parsing the following Accession -- " + id);
        }
    }


}